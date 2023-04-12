import {html, LitElement} from 'lit';

const TAG = "webrtc-support-tag:";

class WebRTCSupport extends LitElement {

    rtcPeerConfig = {
        iceServers: [{urls: "stun:localhost"}]
    };
    rtcPeerConnectionEntries = {};
    userMediaConstraints = {
        audio: false,
        video: {}
    }; // Audio is intentionally left out of this PoC
    stream = null;
    selfVideo = null;
    remoteVideos = {};
    started = false;
    selfId = null;

    render() {
        return html``;
    }

    async start() {
        if (this.started) {
            return; // already started
        }
        if (!this.selfId) {
            throw "No ID has been set";
        }
        console.info(TAG, "Starting WebRTC Support");
        console.info(TAG, "ID is ", this.selfId);

        await this.restartStream();

        const devices = await navigator.mediaDevices.enumerateDevices();
        console.info(TAG, "Available devices: ", devices);

        this.started = true;
        console.info(TAG, "WebRTC Support started")

        await this.$server.updateMediaDevices(devices);
    }

    async restartStream() {
        await this.stopStream();
        await this.startStream();
    }

    async stopStream() {
        if (this.stream !== null) {
            console.info(TAG, "Stopping video stream from own camera");
            this.stream.getTracks().forEach(track => track.stop());
            this.stream = null;
        }
    }

    async startStream() {
        console.info(TAG, "Starting video stream from own camera");
        this.stream = await navigator.mediaDevices.getUserMedia(this.userMediaConstraints);
        await this.addLocalStreamToAllRemoteConnections();
        this.showLocalStreamInViewer();
    }

    async addLocalStreamToRemoteConnectionEntry(entry) {
        const connection = entry.connection;
        if (this.stream) {
            console.info(TAG, "Sending video stream from own camera to", entry.id);
            const videoTrack = this.stream.getTracks().find(track => track.kind === 'video');
            if (entry.videoTrack) {
                await entry.videoTrack.replaceTrack(videoTrack);
            } else {
                entry.videoTrack = connection.addTrack(videoTrack);
            }
        } else {
            console.info(TAG, "No active stream to send to", entry.id);
        }
    }


    async addLocalStreamToAllRemoteConnections() {
        for (let id in this.rtcPeerConnectionEntries) {
            const entry = this.rtcPeerConnectionEntries[id];
            await this.addLocalStreamToRemoteConnectionEntry(entry);
        }
    }

    showLocalStreamInViewer() {
        if (this.selfVideo && this.stream) {
            console.info(TAG, "Showing video stream from own camera in ", this.selfVideo);
            this.selfVideo.srcObject = this.stream;
        } else {
            console.info(TAG, "No active stream or no viewer");
        }
    }

    async changeCamera(deviceId) { // Called from Server
        console.info(TAG, "Changing camera to", deviceId);
        this.userMediaConstraints.video.deviceId = deviceId;
        await this.restartStream();
    }

    showRemoteStream(connectionId, viewer) { // Called from Server
        this.remoteVideos[connectionId] = viewer;
        let entry = this.rtcPeerConnectionEntries[connectionId];
        if (entry && entry.stream) {
            console.info(TAG, "Registering viewer", viewer, "for remote connection ID", connectionId, "and showing video stream", entry.stream);
            viewer.srcObject = entry.stream;
        } else {
            console.info(TAG, "Registering viewer", viewer, "for remote connection ID", connectionId, "without an active stream");
        }
    }

    showSelfVideo(viewer) { // Called from Server
        this.selfVideo = viewer;
        this.showLocalStreamInViewer()
    }

    hideRemoteStream(connectionId) { // Called from Server
        console.info(TAG, "Removing viewer for remote connection ID", connectionId);
        delete this.remoteVideos[connectionId];
    }

    async stop() {
        if (!this.started) {
            return; // Already stopped
        }
        console.info(TAG, "Stopping WebRTC Support");
        await this.stopStream();

        const entryIds = [...Object.keys(this.rtcPeerConnectionEntries)];
        for (let id in entryIds) {
            await this.hangUp(id);
        }
        this.started = false;
        console.info(TAG, "WebRTC Support stopped")
    }

    async invite(targetId) { // Called from Server
        console.info(TAG, "Inviting", targetId);
        const entry = this.createPeerConnectionEntry(targetId, false);
        await this.addLocalStreamToRemoteConnectionEntry(entry);
    }

    async handleInvitation(offer) { // Called from Server
        console.info(TAG, "Handling invitation", offer);
        const targetId = offer.sender;
        const entry = this.getOrCreatePeerConnectionEntry(targetId);
        const connection = entry.connection;
        const offerCollision = entry.makingOffer || connection.signalingState !== 'stable';
        entry.ignoreOffer = !entry.polite && offerCollision;

        if (entry.ignoreOffer) {
            console.info(TAG, "Ignoring invitation", offer);
            return;
        }

        const desc = new RTCSessionDescription(offer.sdp);
        await connection.setRemoteDescription(desc);
        await this.addLocalStreamToRemoteConnectionEntry(entry);
        const answer = await connection.createAnswer();
        await connection.setLocalDescription(answer);
        await this.$server.sendToServer({
            sender: this.selfId,
            target: targetId,
            type: "video-answer",
            sdp: connection.localDescription
        });
    }

    async handleInvitationAnswer(answer) { // Called from Server
        console.info(TAG, "Handling invitation answer", answer);
        const targetId = answer.sender;
        const entry = this.getPeerConnectionEntry(targetId);
        const desc = new RTCSessionDescription(answer.sdp);
        await entry.connection.setRemoteDescription(desc);
    }

    async handleNewICECandidate(msg) { // Called from Server
        console.info(TAG, "Handling new ICE candidate", msg);
        const targetId = msg.sender;
        const entry = this.getPeerConnectionEntry(targetId);
        try {
            const candidate = new RTCIceCandidate(msg.candidate);
            await entry.connection.addIceCandidate(candidate);
        } catch (error) {
            if (!entry.ignoreOffer) {
                throw err;
            }
        }
    }

    async hangUp(targetId) {
        console.info(TAG, "Hanging up connection to", targetId);
        const entry = this.rtcPeerConnectionEntries[targetId];
        if (entry) {
            this.hideRemoteStream(targetId);
            const remoteStream = entry.stream;
            if (remoteStream) {
                remoteStream.getTracks().forEach(track => track.stop());
                await this.$server.remoteStreamRemoved(targetId);
            }
            await entry.connection.close();
            delete this.rtcPeerConnectionEntries[targetId];
        }
    }

    getPeerConnectionEntry(targetId) {
        const entry = this.rtcPeerConnectionEntries[targetId];
        if (!entry) {
            throw "Unknown targetId";
        }
        return entry;
    }

    getOrCreatePeerConnectionEntry(targetId) {
        const entry = this.rtcPeerConnectionEntries[targetId];
        if (!entry) {
            return this.createPeerConnectionEntry(targetId, true);
        } else {
            return entry;
        }
    }

    createPeerConnectionEntry(targetId, polite) {
        const connection = new RTCPeerConnection(this.rtcPeerConfig);
        const entry = {
            id: targetId,
            polite: polite,
            connection: connection,
            makingOffer: false,
            ignoreOffer: false
        };
        connection.onicecandidate = async (event) => {
            if (event.candidate) {
                await this.$server.sendToServer({
                    sender: this.selfId,
                    target: targetId,
                    type: "new-ice-candidate",
                    candidate: event.candidate
                });
            }
        };
        connection.ontrack = async (event) => {
            console.debug(TAG, "ontrack", event);
            let remoteStream;
            if (event.streams[0]) {
                remoteStream = event.streams[0];
            } else {
                remoteStream = new MediaStream();
                remoteStream.addTrack(event.track);
            }

            this.rtcPeerConnectionEntries[targetId].stream = remoteStream;
            const viewer = this.remoteVideos[targetId];
            if (viewer) {
                console.info(TAG, "Showing remote stream", remoteStream, "in viewer", viewer);
                viewer.srcObject = remoteStream;
            } else {
                console.info(TAG, "No viewer available for stream", remoteStream);
            }
            await this.$server.remoteStreamAdded(targetId);
        };
        connection.onnegotiationneeded = async (event) => {
            console.debug(TAG, "onnegotiationneeded", event);
            entry.makingOffer = true;
            try {
                const offer = await connection.createOffer();
                await connection.setLocalDescription(offer);
                await this.$server.sendToServer({
                    sender: this.selfId,
                    target: targetId,
                    type: "video-offer",
                    sdp: connection.localDescription
                });
            } finally {
                entry.makingOffer = false;
            }
        };
        connection.oniceconnectionstatechange = async (event) => {
            console.debug(TAG, "oniceconnectionstatechange", event, "(state:", connection.iceConnectionState, ")");
            switch (connection.iceConnectionState) {
                case "closed":
                case "failed":
                case "disconnected":
                    await this.hangUp(targetId);
                    break;
            }
        };
        connection.onicegatheringstatechange = (event) => {
            console.debug(TAG, "onicegatheringstatechange", event, "(state:", connection.iceGatheringState, ")");
            // NOP for now
        };
        connection.onsignalingstatechange = async (event) => {
            console.debug(TAG, "onsignalingstatechange", event, "(state:", connection.signalingState, ")");
            switch (connection.signalingState) {
                case "closed":
                    await this.hangUp(targetId);
                    break;
            }
        };
        this.rtcPeerConnectionEntries[targetId] = entry;
        return this.rtcPeerConnectionEntries[targetId];
    }

    connectedCallback() {
        super.connectedCallback();
        this.start().catch(error => console.error(TAG, error));
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.stop().catch(error => console.error(TAG, error));
    }
}

window.customElements.define('webrtc-support', WebRTCSupport);

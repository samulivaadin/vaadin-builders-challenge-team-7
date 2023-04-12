import {html, LitElement} from 'lit';

class WebRTCSupport extends LitElement {

    userMediaConstraints = {audio: false, video: {}} // Audio is intentionally left out of this proto
    stream = null;
    remoteVideo = null;
    selfVideo = null;

    render() {
        return html``;
    }

    // TODO Error handling

    async start(remoteVideo, selfVideo) {
        console.log("Showing remote video in ", remoteVideo);
        console.log("Showing self video in ", selfVideo);

        this.remoteVideo = remoteVideo;
        this.selfVideo = selfVideo;

        await this.restartStream();

        const devices = await navigator.mediaDevices.enumerateDevices();
        console.log("Available devices: ", devices);

        return devices;
    }

    async restartStream() {
        await this.stopStream();
        await this.startStream();
    }

    async stopStream() {
        if (this.stream !== null) {
            this.stream.getTracks().forEach(track => track.stop());
            this.stream = null;
        }
    }

    async startStream() {
        this.stream = await navigator.mediaDevices.getUserMedia(this.userMediaConstraints);

        if (this.selfVideo !== null) {
            this.selfVideo.srcObject = this.stream;
        }
    }

    async changeCamera(deviceId) {
        this.userMediaConstraints.video.deviceId = deviceId;
        await this.restartStream();
    }

    async stop() {
        await this.stopStream();
    }

    connectedCallback() {
        super.connectedCallback();
    }

    disconnectedCallback() {
        super.disconnectedCallback();
    }
}

window.customElements.define('webrtc-support', WebRTCSupport);

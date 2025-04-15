function changeVideoSize(size) {
    const video = document.getElementById('currentVideo');
    const videoSource = document.getElementById('videoSource');
    const currentTime = video.currentTime;
    const isPaused = video.paused;

    document.querySelectorAll('.video-controls button').forEach(btn => {
        btn.classList.remove('active');
    });
    document.getElementById(size + 'Btn').classList.add('active');

    if (size === 'tiny') {
        videoSource.src = video.getAttribute('data-tiny');
    } else if (size === 'small') {
        videoSource.src = video.getAttribute('data-small');
    } else if (size === 'medium') {
        videoSource.src = video.getAttribute('data-medium');
    } else if (size === 'large') {
        videoSource.src = video.getAttribute('data-large');
    }

    video.load();
    video.addEventListener('loadedmetadata', function onceLoaded() {
        video.currentTime = currentTime;
        if (!isPaused) video.play();
        video.removeEventListener('loadedmetadata', onceLoaded);
    });
}

window.addEventListener('DOMContentLoaded', () => {
    changeVideoSize('small');
});
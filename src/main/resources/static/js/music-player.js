// 음악 플레이어 관련 JavaScript
document.addEventListener("DOMContentLoaded", () => {
  const musicPlayer = document.getElementById("musicPlayer")

  if (musicPlayer) {
    // 음악 로딩 시 이벤트
    musicPlayer.addEventListener("loadedmetadata", () => {
      musicPlayer.volume = 0.5;
      console.log("Music loaded:", musicPlayer.duration)
    })

    // 재생 시작 이벤트
    musicPlayer.addEventListener("play", () => {
      console.log("Music started playing")
    })

    // 일시 정지 이벤트
    musicPlayer.addEventListener("pause", () => {
      console.log("Music paused")
    })

    // 재생 완료 이벤트
    musicPlayer.addEventListener("ended", () => {
      console.log("Music playback ended")
    })

    // 오류 처리
    musicPlayer.addEventListener("error", (e) => {
      console.error("Error loading music:", e)
      alert("음악을 로드하는 중 오류가 발생했습니다. 다시 시도해주세요.")
    })
  }
})

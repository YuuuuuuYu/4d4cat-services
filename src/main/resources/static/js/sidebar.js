document.addEventListener("DOMContentLoaded", () => {
  const toggleButton = document.getElementById("sidebar-toggle")
  const sidebar = document.querySelector(".sidebar")
  const mainContent = document.querySelector(".main-content")
  const body = document.body

  // 모바일 메뉴 버튼 생성
  createMobileMenuButton()

  // 로컬 스토리지에서 사이드바 상태 불러오기
  const sidebarCollapsed = localStorage.getItem("sidebar-collapsed") === "true"

  // 초기 상태 설정 (모바일이 아닐 때만)
  if (sidebarCollapsed && window.innerWidth > 768) {
    sidebar.classList.add("collapsed")
    mainContent.classList.add("sidebar-collapsed")
    body.classList.add("sidebar-collapsed")
  }

  // 토글 버튼 클릭 이벤트
  if (toggleButton) {
    toggleButton.addEventListener("click", () => {
      sidebar.classList.toggle("collapsed")
      mainContent.classList.toggle("sidebar-collapsed")
      body.classList.toggle("sidebar-collapsed")

      // 상태 저장 (모바일이 아닐 때만)
      if (window.innerWidth > 768) {
        const isCollapsed = sidebar.classList.contains("collapsed")
        localStorage.setItem("sidebar-collapsed", isCollapsed)
      }
    })
  }

  // 모바일 메뉴 버튼 생성 함수
  function createMobileMenuButton() {
    if (!document.querySelector(".mobile-menu-button")) {
      const mobileMenuButton = document.createElement("button")
      mobileMenuButton.className = "mobile-menu-button"
      mobileMenuButton.innerHTML = `
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="3" y1="12" x2="21" y2="12"></line>
          <line x1="3" y1="6" x2="21" y2="6"></line>
          <line x1="3" y1="18" x2="21" y2="18"></line>
        </svg>
      `
      document.body.appendChild(mobileMenuButton)

      // 모바일 메뉴 버튼 클릭 이벤트
      mobileMenuButton.addEventListener("click", () => {
        sidebar.classList.toggle("mobile-open")

        // 오버레이 처리
        const overlay = document.querySelector(".sidebar-overlay")
        if (overlay) {
          overlay.classList.toggle("active")
        }
      })
    }
  }

  // 모바일에서 오버레이 클릭 시 사이드바 닫기
  const createOverlay = () => {
    if (!document.querySelector(".sidebar-overlay")) {
      const overlay = document.createElement("div")
      overlay.className = "sidebar-overlay"
      document.body.appendChild(overlay)

      overlay.addEventListener("click", () => {
        sidebar.classList.remove("mobile-open")
        overlay.classList.remove("active")
      })
    }
  }

  // 모바일 환경 체크
  if (window.innerWidth <= 768) {
    createOverlay()
  }

  // 화면 크기 변경 감지
  window.addEventListener("resize", () => {
    if (window.innerWidth <= 768) {
      createOverlay()
      // 모바일로 전환 시 사이드바 상태 초기화
      sidebar.classList.remove("collapsed")
      mainContent.classList.remove("sidebar-collapsed")
      body.classList.remove("sidebar-collapsed")
      sidebar.classList.remove("mobile-open")
      const overlay = document.querySelector(".sidebar-overlay")
      if (overlay) {
        overlay.classList.remove("active")
      }
    } else {
      // 데스크톱으로 전환 시 모바일 메뉴 상태 초기화
      sidebar.classList.remove("mobile-open")
      const overlay = document.querySelector(".sidebar-overlay")
      if (overlay) {
        overlay.classList.remove("active")
      }

      // 저장된 사이드바 상태 적용
      const sidebarCollapsed = localStorage.getItem("sidebar-collapsed") === "true"
      if (sidebarCollapsed) {
        sidebar.classList.add("collapsed")
        mainContent.classList.add("sidebar-collapsed")
        body.classList.add("sidebar-collapsed")
      } else {
        sidebar.classList.remove("collapsed")
        mainContent.classList.remove("sidebar-collapsed")
        body.classList.remove("sidebar-collapsed")
      }
    }
  })

  // 사이드바 탭 클릭 이벤트 (모바일에서 클릭 시 사이드바 닫기)
  const sidebarTabs = document.querySelectorAll(".sidebar-tab")
  sidebarTabs.forEach((tab) => {
    tab.addEventListener("click", () => {
      if (window.innerWidth <= 768) {
        setTimeout(() => {
          sidebar.classList.remove("mobile-open")
          const overlay = document.querySelector(".sidebar-overlay")
          if (overlay) {
            overlay.classList.remove("active")
          }
        }, 100) // 약간의 지연을 두어 링크 이동이 먼저 처리되도록 함
      }
    })
  })
})

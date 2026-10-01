document.addEventListener('DOMContentLoaded', () => {
  // 1. Navbar Glassmorphism no Scroll
  const navbar = document.getElementById('navbar');
  window.addEventListener('scroll', () => {
    if (window.scrollY > 24) {
      navbar.style.borderBottom = '1px solid #e2e8f0';
      navbar.style.backgroundColor = 'rgba(255, 255, 255, 0.9)';
      navbar.style.backdropFilter = 'blur(16px)';
      navbar.style.paddingTop = '12px';
      navbar.style.paddingBottom = '12px';
      navbar.style.boxShadow = '0 1px 3px 0 rgba(0, 0, 0, 0.1)';
    } else {
      navbar.style.borderBottom = 'none';
      navbar.style.backgroundColor = 'transparent';
      navbar.style.backdropFilter = 'none';
      navbar.style.paddingTop = '20px';
      navbar.style.paddingBottom = '20px';
      navbar.style.boxShadow = 'none';
    }
  });

  // 2. Menu Mobile Toggle
  const mobileToggle = document.getElementById('mobile-toggle');
  const mobileMenu = document.getElementById('mobile-menu');
  const menuIcon = document.getElementById('menu-icon');

  mobileToggle.addEventListener('click', () => {
    mobileMenu.classList.toggle('hidden');
    if (mobileMenu.classList.contains('hidden')) {
      menuIcon.className = 'fa-solid fa-bars';
    } else {
      menuIcon.className = 'fa-solid fa-xmark';
    }
  });

  // Fechar menu mobile ao clicar nos links
  document.querySelectorAll('.mobile-link').forEach(link => {
    link.addEventListener('click', () => {
      mobileMenu.classList.add('hidden');
      menuIcon.className = 'fa-solid fa-bars';
    });
  });

  // 3. Scroll Reveal Observer (Aparição suave de elementos)
  const revealElements = document.querySelectorAll('.reveal');
  const revealObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-visible');
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: 0.12 });

  revealElements.forEach(el => revealObserver.observe(el));

  // 4. Simulador Hero Progress Bar Interativo
  let currentProgress = 0;
  setTimeout(() => {
    const interval = setInterval(() => {
      if (currentProgress >= 78) {
        clearInterval(interval);
      } else {
        currentProgress += 1;
        const bar = document.getElementById('progress-bar');
        const text = document.getElementById('progress-text');
        if (bar && text) {
          bar.style.width = currentProgress + '%';
          text.textContent = currentProgress + '%';
        }
      }
    }, 30);
  }, 800);

  // 5. Animador de Contadores (Impact Counters)
  const counterSection = document.getElementById('counter-section');
  let counted = false;

  const runCounters = () => {
    if (counted) return;
    const stats = [
      { el: document.getElementById('stat-1'), target: 3420000 },
      { el: document.getElementById('stat-2'), target: 890000 },
      { el: document.getElementById('stat-3'), target: 12847 },
      { el: document.getElementById('stat-4'), target: 285000 }
    ];

    stats.forEach(stat => {
      if (!stat.el) return;
      const target = stat.target;
      const duration = 2200;
      const startTime = performance.now();

      const updateCount = (now) => {
        const elapsed = now - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3);
        const currentVal = Math.floor(target * eased);

        stat.el.textContent = currentVal.toLocaleString('pt-BR');

        if (progress < 1) {
          requestAnimationFrame(updateCount);
        } else {
          stat.el.textContent = target.toLocaleString('pt-BR');
        }
      };
      requestAnimationFrame(updateCount);
    });
    counted = true;
  };

  const counterObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        runCounters();
      }
    });
  }, { threshold: 0.3 });

  if (counterSection) {
    counterObserver.observe(counterSection);
  }
});

document.addEventListener("DOMContentLoaded", function () {
  const observerOptions = {
    root: null,
    rootMargin: '0px',
    threshold: 0.25 // Dispara quando 25% da secção estiver visível no ecrã
  };


  //!--Script para ativar a animação exatamente quando o utilizador faz scroll até à secção-- >
  const observer = new IntersectionObserver((entries, observer) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const cards = entry.target.querySelectorAll('.cycle-card');
        cards.forEach(card => {
          card.classList.remove('opacity-0', 'translate-y-8');
          card.classList.add('opacity-100', 'translate-y-0');
        });
        observer.unobserve(entry.target); // Executa apenas uma vez ao chegar
      }
    });
  }, observerOptions);

  const targetSection = document.getElementById('como-funciona');
  if (targetSection) {
    observer.observe(targetSection);
  }
});
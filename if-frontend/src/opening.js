// opening.js – controls the cinematic opening sequence

document.addEventListener('DOMContentLoaded', () => {
  const scene = document.getElementById('scene');
  const subtitle = document.createElement('div');
  subtitle.className = 'subtitle';
  document.body.appendChild(subtitle);

  const timeline = [
    // initial black screen is implicit (no text for 2 s)
    { text: 'Cada día una familia se rompe.', duration: 4, subtitle: false },
    { text: 'Y casi siempre nadie se da cuenta.', duration: 4, subtitle: false },
    { text: 'No porque no se amen.', duration: 3, subtitle: false },
    { text: 'Sino porque dejaron de escucharse.', duration: 3, subtitle: false },
    { text: 'Dejaron de compartir tiempo.', duration: 3, subtitle: false },
    { text: 'Dejaron de entenderse.', duration: 3, subtitle: false },
    {
      // oral narration block – displayed as subtitle at the bottom
      text: `Las crisis familiares rara vez empiezan con grandes acontecimientos.
Generalmente comienzan con pequeñas desconexiones que nadie detecta a tiempo.`,
      duration: 8,
      subtitle: true,
    },
  ];

  // Helper to show a line
  function showLine(entry) {
    const durationSec = `${entry.duration}s`;
    const target = entry.subtitle ? subtitle : scene;
    target.style.setProperty('--duration', durationSec);
    target.textContent = entry.text;
    target.classList.remove('visible');
    void target.offsetWidth; // force reflow
    target.classList.add('visible');
  }

  // Start the sequence after the initial 2 s black screen
  let offset = 2000; // ms
  timeline.forEach((entry) => {
    setTimeout(() => {
      scene.textContent = '';
      subtitle.textContent = '';
      showLine(entry);
    }, offset);
    offset += entry.duration * 1000;
  });

  // After the last entry, wait its duration then redirect to a simple "finished" page
  const totalDuration = offset + (timeline[timeline.length - 1].duration * 1000);
  setTimeout(() => {
    // Clear the screen
    scene.textContent = '';
    subtitle.textContent = '';
    // Redirect to a minimal finished page
    window.location.href = '/done.html';
  }, totalDuration);
});

document.addEventListener("DOMContentLoaded", function () {
  if (typeof mermaid === "undefined") return;

  mermaid.initialize({startOnLoad: false, theme: "dark"});

  var diagrams = Array.from(document.querySelectorAll("pre.mermaid[data-src]"));
  Promise.all(diagrams.map(function (pre) {
    return fetch(pre.dataset.src).then(function (response) {
      return response.text();
    }).then(function (source) {
      pre.textContent = source;
    });
  })).then(function () {
    return mermaid.run();
  }).then(function () {
    document.querySelectorAll(".mermaid svg").forEach(function (svg) {
      svg.style.maxWidth = "none";
      svg.style.cursor = "pointer";
      svg.setAttribute("title", "Click to expand");
      svg.addEventListener("click", function () {
        openDiagramLightbox(svg);
      });
    });
  });
});

function openDiagramLightbox(sourceSvg) {
  var overlay = document.createElement("div");
  overlay.style.cssText =
    "position:fixed; inset:0; z-index:9999; background:rgba(0,0,0,0.85);" +
    "display:flex; align-items:center; justify-content:center; padding:2rem;";

  var container = document.createElement("div");
  container.style.cssText =
    "width:90vw; height:90vh; background:#12161b; border:1px solid #5fd0a8;" +
    "border-radius:8px; overflow:hidden; position:relative;";

  var clone = sourceSvg.cloneNode(true);
  clone.style.width = "100%";
  clone.style.height = "100%";
  clone.style.maxWidth = "none";
  clone.style.cursor = "grab";
  clone.removeAttribute("title");

  var closeBtn = document.createElement("button");
  closeBtn.textContent = "×";
  closeBtn.style.cssText =
    "position:absolute; top:0.5rem; right:0.75rem; z-index:10;" +
    "background:none; border:none; color:#5fd0a8; font-size:2rem;" +
    "cursor:pointer; line-height:1;";

  var hint = document.createElement("div");
  hint.textContent = "Scroll/double-click to zoom · Drag to pan · ESC to close";
  hint.style.cssText =
    "position:absolute; bottom:0.5rem; left:50%; transform:translateX(-50%);" +
    "color:#8b96a3; font-size:0.75rem; pointer-events:none; white-space:nowrap;";

  container.appendChild(clone);
  container.appendChild(closeBtn);
  container.appendChild(hint);
  overlay.appendChild(container);
  document.body.appendChild(overlay);

  var pz = null;
  if (typeof svgPanZoom !== "undefined") {
    pz = svgPanZoom(clone, {
      zoomEnabled: true,
      controlIconsEnabled: true,
      zoomScaleSensitivity: 0.4,
      dblClickZoomEnabled: true,
      mouseWheelZoomEnabled: true,
      fit: true,
      center: true
    });
  }

  function close() {
    if (pz) pz.destroy();
    document.body.removeChild(overlay);
    document.removeEventListener("keydown", onKey);
  }

  function onKey(e) {
    if (e.key === "Escape") close();
  }

  closeBtn.addEventListener("click", close);
  overlay.addEventListener("click", function (e) {
    if (e.target === overlay) close();
  });
  document.addEventListener("keydown", onKey);
}

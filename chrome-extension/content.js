// SourceVault Content Script - Inspects active tab DOM and performance resources
(() => {
  function inspectPageResources() {
    const resources = [];
    const seenUrls = new Set();

    function add(url, type) {
      if (!url || url.startsWith('data:') || seenUrls.has(url)) return;
      seenUrls.add(url);
      resources.push({
        url: url,
        resourceType: type,
        httpStatus: 200,
        contentType: getContentType(type)
      });
    }

    function getContentType(type) {
      switch (type) {
        case 'SCRIPT': return 'application/javascript';
        case 'STYLESHEET': return 'text/css';
        case 'IMAGE': return 'image/png';
        case 'FONT': return 'font/woff2';
        case 'HTML': return 'text/html';
        default: return 'application/octet-stream';
      }
    }

    // 1. Current document
    add(window.location.href, 'HTML');

    // 2. Scripts
    document.querySelectorAll('script[src]').forEach(el => {
      add(el.src, 'SCRIPT');
    });

    // 3. Stylesheets
    document.querySelectorAll('link[rel~="stylesheet"]').forEach(el => {
      add(el.href, 'STYLESHEET');
    });

    // 4. Icons & Images
    document.querySelectorAll('link[rel*="icon"]').forEach(el => {
      add(el.href, 'IMAGE');
    });
    document.querySelectorAll('img[src]').forEach(el => {
      add(el.src, 'IMAGE');
    });

    // 5. Performance timing entries
    if (window.performance && window.performance.getEntriesByType) {
      const perfEntries = window.performance.getEntriesByType('resource');
      perfEntries.forEach(entry => {
        const url = entry.name;
        const initiator = entry.initiatorType;
        let type = 'OTHER';
        if (initiator === 'script' || url.endsWith('.js')) type = 'SCRIPT';
        else if (initiator === 'css' || initiator === 'link' || url.endsWith('.css')) type = 'STYLESHEET';
        else if (initiator === 'img' || /\.(png|jpe?g|svg|webp|gif|ico)$/i.test(url)) type = 'IMAGE';
        else if (/\.(woff2?|ttf|otf|eot)$/i.test(url)) type = 'FONT';
        add(url, type);
      });
    }

    return {
      url: window.location.href,
      title: document.title,
      resources: resources
    };
  }

  // Listen for messages from popup
  chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.action === 'INSPECT_RESOURCES') {
      const data = inspectPageResources();
      sendResponse(data);
    }
    return true;
  });
})();

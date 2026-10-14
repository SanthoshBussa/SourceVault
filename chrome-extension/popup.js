document.addEventListener('DOMContentLoaded', async () => {
  const statusBadge = document.getElementById('statusBadge');
  const serverUrlInput = document.getElementById('serverUrl');
  const btnCheckHealth = document.getElementById('btnCheckHealth');
  const tabTitle = document.getElementById('tabTitle');
  const tabUrl = document.getElementById('tabUrl');
  const btnInspect = document.getElementById('btnInspect');
  const resultsCard = document.getElementById('resultsCard');
  const resultsCount = document.getElementById('resultsCount');
  const resourceList = document.getElementById('resourceList');
  const btnSync = document.getElementById('btnSync');
  const msgBox = document.getElementById('msgBox');

  let currentTab = null;
  let inspectedResources = [];
  let sessionId = null;

  // Load stored server URL
  chrome.storage.local.get(['serverUrl', 'sessionId'], (res) => {
    if (res.serverUrl) {
      serverUrlInput.value = res.serverUrl;
    }
    if (res.sessionId) {
      sessionId = res.sessionId;
    }
    checkBackendHealth();
  });

  // Query active tab
  chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
    if (tabs && tabs[0]) {
      currentTab = tabs[0];
      tabTitle.textContent = currentTab.title || 'Untitled';
      tabUrl.textContent = currentTab.url || '-';
    }
  });

  btnCheckHealth.addEventListener('click', checkBackendHealth);

  async function checkBackendHealth() {
    const baseUrl = serverUrlInput.value.trim().replace(/\/+$/, '');
    chrome.storage.local.set({ serverUrl: baseUrl });
    statusBadge.textContent = 'Connecting...';
    statusBadge.className = 'badge';

    try {
      const resp = await fetch(`${baseUrl}/api/v1/browser/status`);
      if (resp.ok) {
        statusBadge.textContent = 'Connected';
        statusBadge.className = 'badge badge-connected';
      } else {
        throw new Error('HTTP ' + resp.status);
      }
    } catch (e) {
      statusBadge.textContent = 'Disconnected';
      statusBadge.className = 'badge badge-disconnected';
    }
  }

  btnInspect.addEventListener('click', () => {
    if (!currentTab || !currentTab.id) return;
    msgBox.textContent = 'Inspecting active tab...';
    msgBox.style.color = '#4338ca';

    chrome.tabs.sendMessage(currentTab.id, { action: 'INSPECT_RESOURCES' }, (response) => {
      if (chrome.runtime.lastError || !response) {
        msgBox.textContent = 'Could not inspect tab. Refresh tab and try again.';
        msgBox.style.color = '#dc2626';
        return;
      }

      inspectedResources = response.resources || [];
      resultsCount.textContent = `Found ${inspectedResources.length} Resources`;
      resourceList.innerHTML = '';

      inspectedResources.forEach((res) => {
        const item = document.createElement('div');
        item.className = 'res-item';
        const name = res.url.split('/').pop().split('?')[0] || res.url;
        item.innerHTML = `<span title="${res.url}">${name.substring(0, 32)}</span><span class="res-type">${res.resourceType}</span>`;
        resourceList.appendChild(item);
      });

      resultsCard.style.display = 'block';
      msgBox.textContent = 'Tab inspected successfully!';
      msgBox.style.color = '#059669';
    });
  });

  btnSync.addEventListener('click', async () => {
    const baseUrl = serverUrlInput.value.trim().replace(/\/+$/, '');
    if (!currentTab || inspectedResources.length === 0) return;

    btnSync.disabled = true;
    btnSync.textContent = 'Syncing...';
    msgBox.textContent = 'Registering browser session...';

    try {
      // 1. Register Session
      const sessionPayload = {
        sessionId: sessionId,
        browserName: 'Chrome',
        extensionVersion: '1.0.0',
        activeTabUrl: currentTab.url,
        activeTabTitle: currentTab.title
      };

      const sessResp = await fetch(`${baseUrl}/api/v1/browser/sessions`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(sessionPayload)
      });

      if (!sessResp.ok) throw new Error('Session registration failed');
      const sessionData = await sessResp.json();
      sessionId = sessionData.sessionId;
      chrome.storage.local.set({ sessionId });

      // 2. Submit Resources
      msgBox.textContent = `Uploading ${inspectedResources.length} resource references...`;
      const resResp = await fetch(`${baseUrl}/api/v1/browser/resources?sessionId=${encodeURIComponent(sessionId)}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(inspectedResources)
      });

      if (!resResp.ok) throw new Error('Resource upload failed');

      msgBox.textContent = 'Successfully synced with SourceVault!';
      msgBox.style.color = '#059669';
      btnSync.textContent = 'Synced ✓';
    } catch (err) {
      msgBox.textContent = 'Sync error: ' + err.message;
      msgBox.style.color = '#dc2626';
      btnSync.disabled = false;
      btnSync.textContent = 'Retry Sync';
    }
  });
});

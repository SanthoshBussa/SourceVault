// SourceVault Service Worker
chrome.runtime.onInstalled.addListener(() => {
  console.log('SourceVault Collector Extension Installed');
});

// Periodic keepalive / heartbeat helper if needed
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.action === 'PING') {
    sendResponse({ status: 'PONG' });
  }
  return true;
});

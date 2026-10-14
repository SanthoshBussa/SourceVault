import React from 'react';
import { X, Chrome, CheckCircle2, AlertTriangle, ExternalLink, RefreshCw } from 'lucide-react';

interface Props {
  onClose: () => void;
  browserStatus: any;
}

export const BrowserSetupModal: React.FC<Props> = ({ onClose, browserStatus }) => {
  const isConnected = browserStatus?.connected;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-fadeIn">
      <div className="bg-white rounded-2xl max-w-xl w-full p-6 shadow-2xl border border-slate-200 relative">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-100 transition"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="flex items-center gap-3 mb-4">
          <div className="w-10 h-10 rounded-xl bg-brand-50 border border-brand-200 flex items-center justify-center text-brand-700">
            <Chrome className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-lg font-bold text-slate-900">Chrome Extension Integration</h3>
            <p className="text-xs text-slate-500">Capture authenticated browser sessions and virtual source maps</p>
          </div>
        </div>

        <div className={`p-4 rounded-xl border mb-5 ${
          isConnected ? 'bg-emerald-50 border-emerald-200' : 'bg-slate-50 border-slate-200'
        }`}>
          <div className="flex items-start gap-3">
            {isConnected ? (
              <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
            ) : (
              <AlertTriangle className="w-5 h-5 text-amber-500 shrink-0 mt-0.5" />
            )}
            <div>
              <p className={`text-sm font-semibold ${isConnected ? 'text-emerald-900' : 'text-slate-800'}`}>
                {isConnected ? 'Extension Connected & Ready' : 'Extension Currently Disconnected'}
              </p>
              <p className="text-xs text-slate-600 mt-1">
                {isConnected
                  ? `Active tab detected: ${browserStatus.activeTabTitle || 'Website'} (${browserStatus.activeTabUrl})`
                  : 'SourceVault is operating in URL-Only Public Mode. To collect behind logins or inspect live network resources, load the extension below.'}
              </p>
            </div>
          </div>
        </div>

        <div className="space-y-3.5 mb-6 text-sm">
          <h4 className="font-semibold text-slate-900 text-xs uppercase tracking-wider text-slate-500">
            One-Time Setup Instructions
          </h4>

          <div className="flex gap-3">
            <span className="w-6 h-6 rounded-full bg-brand-100 text-brand-700 text-xs font-bold flex items-center justify-center shrink-0">1</span>
            <div>
              <p className="font-medium text-slate-900">Open Chrome Extension Manager</p>
              <p className="text-xs text-slate-500">Navigate to <code className="bg-slate-100 px-1 py-0.5 rounded text-brand-700">chrome://extensions/</code> in your Chrome address bar.</p>
            </div>
          </div>

          <div className="flex gap-3">
            <span className="w-6 h-6 rounded-full bg-brand-100 text-brand-700 text-xs font-bold flex items-center justify-center shrink-0">2</span>
            <div>
              <p className="font-medium text-slate-900">Enable Developer Mode & Load Unpacked</p>
              <p className="text-xs text-slate-500">Toggle on <strong>Developer Mode</strong> in the top right, click <strong>Load unpacked</strong>, and select the folder:</p>
              <div className="mt-1 bg-slate-100 p-2 rounded text-xs font-mono text-slate-800 break-all select-all">
                {window.location.origin.includes('localhost') ? 'c:\\Users\\iamsa\\OneDrive\\Desktop\\MyAiFiles\\chrome-extension' : './chrome-extension'}
              </div>
            </div>
          </div>

          <div className="flex gap-3">
            <span className="w-6 h-6 rounded-full bg-brand-100 text-brand-700 text-xs font-bold flex items-center justify-center shrink-0">3</span>
            <div>
              <p className="font-medium text-slate-900">Click Extension & Sync</p>
              <p className="text-xs text-slate-500">Open any target site tab, click the SourceVault extension icon in your toolbar, and click <strong>Sync to SourceVault</strong>.</p>
            </div>
          </div>
        </div>

        <div className="flex justify-end gap-3 pt-3 border-t border-slate-100">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-slate-900 text-white rounded-lg text-sm font-semibold hover:bg-slate-800 transition"
          >
            Got it
          </button>
        </div>
      </div>
    </div>
  );
};

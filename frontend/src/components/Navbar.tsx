import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Archive, History, Chrome, Radio, ShieldCheck } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { getBrowserStatus } from '../api';
import { BrowserSetupModal } from './BrowserSetupModal';

export const Navbar: React.FC = () => {
  const location = useLocation();
  const [showBrowserModal, setShowBrowserModal] = useState(false);

  const { data: browserStatus } = useQuery({
    queryKey: ['browserStatus'],
    queryFn: getBrowserStatus,
    refetchInterval: 5000
  });

  const isConnected = browserStatus?.connected;

  return (
    <>
      <header className="sticky top-0 z-40 w-full border-b border-slate-200 bg-white/95 backdrop-blur">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-8">
            <Link to="/" className="flex items-center gap-2.5 group">
              <div className="w-10 h-10 rounded-xl bg-brand-700 flex items-center justify-center text-white shadow-sm group-hover:bg-brand-800 transition">
                <Archive className="w-5 h-5" />
              </div>
              <div>
                <span className="font-bold text-lg text-slate-900 tracking-tight flex items-center gap-1.5">
                  SourceVault
                  <span className="text-[10px] uppercase font-semibold tracking-wider bg-brand-50 text-brand-700 px-1.5 py-0.5 rounded border border-brand-200">
                    Pro
                  </span>
                </span>
                <p className="text-xs text-slate-500 font-medium">Source Collector & ZIP Backup</p>
              </div>
            </Link>

            <nav className="hidden md:flex items-center gap-1">
              <Link
                to="/"
                className={`px-3 py-2 rounded-lg text-sm font-medium transition ${
                  location.pathname === '/' ? 'bg-slate-100 text-slate-900 font-semibold' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                Dashboard
              </Link>
              <Link
                to="/history"
                className={`px-3 py-2 rounded-lg text-sm font-medium transition flex items-center gap-1.5 ${
                  location.pathname.startsWith('/history') ? 'bg-slate-100 text-slate-900 font-semibold' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                <History className="w-4 h-4" />
                Job History
              </Link>
            </nav>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => setShowBrowserModal(true)}
              className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium border transition ${
                isConnected
                  ? 'bg-emerald-50 text-emerald-800 border-emerald-200 hover:bg-emerald-100'
                  : 'bg-amber-50 text-amber-800 border-amber-200 hover:bg-amber-100'
              }`}
            >
              <span className="relative flex h-2 w-2">
                <span
                  className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${
                    isConnected ? 'bg-emerald-400' : 'bg-amber-400'
                  }`}
                />
                <span
                  className={`relative inline-flex rounded-full h-2 w-2 ${
                    isConnected ? 'bg-emerald-500' : 'bg-amber-500'
                  }`}
                />
              </span>
              <Chrome className="w-3.5 h-3.5" />
              <span>{isConnected ? 'Chrome Connected' : 'Browser Offline (URL Mode)'}</span>
            </button>

            <div className="hidden sm:flex items-center gap-1.5 text-xs text-slate-500 bg-slate-100 px-2.5 py-1.5 rounded-lg border border-slate-200">
              <ShieldCheck className="w-4 h-4 text-emerald-600" />
              <span>SSRF Guard Active</span>
            </div>
          </div>
        </div>
      </header>

      {showBrowserModal && <BrowserSetupModal onClose={() => setShowBrowserModal(false)} browserStatus={browserStatus} />}
    </>
  );
};

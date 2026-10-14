import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Globe,
  Search,
  ArrowRight,
  Download,
  FolderArchive,
  Layers,
  HardDrive,
  ShieldAlert,
  Clock,
  Sparkles,
  ExternalLink,
  CheckCircle2,
  AlertCircle
} from 'lucide-react';
import { fetchStats, listJobs, formatBytes } from '../api';

export const DashboardPage: React.FC = () => {
  const [url, setUrl] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();

  const { data: stats } = useQuery({
    queryKey: ['stats'],
    queryFn: fetchStats,
    refetchInterval: 10000
  });

  const { data: recentJobs } = useQuery({
    queryKey: ['recentJobs'],
    queryFn: () => listJobs(0, 5),
    refetchInterval: 10000
  });

  const handleAnalyze = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const trimmed = url.trim();
    if (!trimmed) {
      setError('Please enter a website URL.');
      return;
    }

    try {
      let testUrl = trimmed;
      if (!testUrl.startsWith('http://') && !testUrl.startsWith('https://')) {
        testUrl = 'https://' + testUrl;
      }
      const parsed = new URL(testUrl);
      if (!parsed.hostname.includes('.')) {
        setError('Please enter a valid website hostname (e.g. topbrains.com).');
        return;
      }
    } catch {
      setError('Invalid URL format. Please provide a valid HTTP or HTTPS address.');
      return;
    }

    setIsSubmitting(true);
    navigate(`/analysis?url=${encodeURIComponent(trimmed)}`);
  };

  return (
    <div className="space-y-10 pb-16">
      {/* Hero Section */}
      <section className="text-center pt-8 max-w-3xl mx-auto space-y-4">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-brand-50 border border-brand-200 text-brand-700 text-xs font-semibold shadow-xs">
          <Sparkles className="w-3.5 h-3.5" />
          <span>Chrome Source Collector & Reconstructor</span>
        </div>
        <h1 className="text-4xl sm:text-5xl font-extrabold text-slate-900 tracking-tight leading-tight">
          Collect. Package. <span className="text-brand-700">Download.</span>
        </h1>
        <p className="text-base sm:text-lg text-slate-600 max-w-2xl mx-auto leading-relaxed">
          Inspect any website, extract scripts, styles, fonts, and assets, automatically reconstruct original source files from source maps, and download a validated ZIP archive.
        </p>

        {/* Primary URL Input Card */}
        <div className="pt-4 max-w-2xl mx-auto">
          <form onSubmit={handleAnalyze} className="relative">
            <div className="flex flex-col sm:flex-row items-stretch gap-2.5 p-2 bg-white rounded-2xl shadow-xl shadow-slate-200/50 border border-slate-200 focus-within:ring-2 focus-within:ring-brand-500 focus-within:border-transparent transition">
              <div className="relative flex-1 flex items-center pl-3">
                <Globe className="w-5 h-5 text-slate-400 shrink-0" />
                <input
                  type="text"
                  value={url}
                  onChange={(e) => {
                    setUrl(e.target.value);
                    if (error) setError(null);
                  }}
                  placeholder="https://topbrains.com/"
                  className="w-full px-3 py-3 text-slate-900 placeholder-slate-400 text-base bg-transparent border-none focus:outline-none"
                  autoFocus
                />
              </div>
              <button
                type="submit"
                disabled={isSubmitting}
                className="px-6 py-3.5 bg-brand-700 hover:bg-brand-800 active:bg-brand-900 text-white font-semibold rounded-xl text-sm flex items-center justify-center gap-2 shadow-sm transition disabled:opacity-75 cursor-pointer shrink-0"
              >
                <Search className="w-4 h-4" />
                <span>{isSubmitting ? 'Analyzing...' : 'Analyze Website'}</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
            {error && (
              <div className="flex items-center gap-1.5 text-xs text-rose-600 mt-2 px-3 text-left font-medium">
                <AlertCircle className="w-3.5 h-3.5" />
                <span>{error}</span>
              </div>
            )}
          </form>
          <div className="flex items-center justify-center gap-4 text-xs text-slate-500 mt-3">
            <span>Try sample URLs:</span>
            <button
              onClick={() => setUrl('https://topbrains.com/')}
              className="text-brand-700 hover:underline font-medium"
            >
              https://topbrains.com/
            </button>
            <span className="text-slate-300">•</span>
            <button
              onClick={() => setUrl('https://example.com')}
              className="text-brand-700 hover:underline font-medium"
            >
              https://example.com
            </button>
          </div>
        </div>
      </section>

      {/* Metrics Row */}
      <section className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-5 bg-white rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Resources Collected</span>
            <Layers className="w-4 h-4 text-brand-600" />
          </div>
          <p className="text-2xl font-bold text-slate-900">
            {stats?.totalDownloaded?.toLocaleString() || 0}
          </p>
          <p className="text-xs text-slate-500 mt-1">Across all historical jobs</p>
        </div>

        <div className="p-5 bg-white rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Completed Archives</span>
            <FolderArchive className="w-4 h-4 text-emerald-600" />
          </div>
          <p className="text-2xl font-bold text-slate-900">
            {stats?.totalArchives?.toLocaleString() || 0}
          </p>
          <p className="text-xs text-slate-500 mt-1">Validated ZIP packages</p>
        </div>

        <div className="p-5 bg-white rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Storage Utilized</span>
            <HardDrive className="w-4 h-4 text-violet-600" />
          </div>
          <p className="text-2xl font-bold text-slate-900">
            {formatBytes(stats?.totalStorageBytes || 0)}
          </p>
          <p className="text-xs text-slate-500 mt-1">Persisted locally</p>
        </div>

        <div className="p-5 bg-white rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Browser Mode</span>
            <CheckCircle2 className="w-4 h-4 text-amber-500" />
          </div>
          <p className="text-base font-bold text-slate-900 truncate">
            {stats?.browserStatus?.connected ? 'Extension Connected' : 'Public URL-Only'}
          </p>
          <p className="text-xs text-slate-500 mt-1">
            {stats?.browserStatus?.connected ? 'Active tab capture enabled' : 'Safe server-side scanner'}
          </p>
        </div>
      </section>

      {/* Recent Collection Jobs Table */}
      <section className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h3 className="font-bold text-base text-slate-900">Recent Collection Jobs</h3>
            <p className="text-xs text-slate-500">History saved in persistent database</p>
          </div>
          <Link
            to="/history"
            className="text-xs font-semibold text-brand-700 hover:text-brand-800 flex items-center gap-1 transition"
          >
            <span>View All Jobs</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {recentJobs?.content && recentJobs.content.length > 0 ? (
          <div className="divide-y divide-slate-100">
            {recentJobs.content.map((job) => (
              <div
                key={job.id}
                className="px-6 py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:bg-slate-50 transition"
              >
                <div className="space-y-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="font-semibold text-sm text-slate-900 truncate">{job.websiteUrl}</span>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                        job.status === 'COMPLETED'
                          ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                          : job.status === 'COMPLETED_WITH_WARNINGS'
                          ? 'bg-amber-50 text-amber-700 border border-amber-200'
                          : job.status === 'COLLECTING' || job.status === 'ARCHIVING'
                          ? 'bg-brand-50 text-brand-700 border border-brand-200 animate-pulse'
                          : 'bg-slate-100 text-slate-700'
                      }`}
                    >
                      {job.status}
                    </span>
                  </div>
                  <div className="flex items-center gap-4 text-xs text-slate-500">
                    <span className="flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      {new Date(job.createdAt).toLocaleString()}
                    </span>
                    <span>{job.totalDownloaded} files</span>
                    <span>{formatBytes(job.archiveSize)}</span>
                  </div>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  {job.status === 'COLLECTING' || job.status === 'ARCHIVING' ? (
                    <Link
                      to={`/collection/${job.id}`}
                      className="px-3 py-1.5 bg-brand-700 hover:bg-brand-800 text-white rounded-lg text-xs font-semibold transition"
                    >
                      Monitor Live
                    </Link>
                  ) : (
                    <Link
                      to={`/results/${job.id}`}
                      className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-800 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition"
                    >
                      <span>View Results</span>
                      <ArrowRight className="w-3 h-3" />
                    </Link>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="py-12 text-center text-slate-500 space-y-2">
            <FolderArchive className="w-8 h-8 mx-auto text-slate-300" />
            <p className="text-sm font-medium">No previous collection jobs found.</p>
            <p className="text-xs text-slate-400">Enter a website URL above to initiate your first backup.</p>
          </div>
        )}
      </section>

      {/* Security & Limitations Callout Card */}
      <section className="bg-slate-900 text-slate-200 rounded-2xl p-6 sm:p-8 space-y-4">
        <div className="flex items-center gap-2.5 text-brand-300 font-semibold text-sm">
          <ShieldAlert className="w-5 h-5 text-brand-400" />
          <span>Security & Collection Capabilities Note</span>
        </div>
        <div className="grid md:grid-cols-2 gap-6 text-xs text-slate-300 leading-relaxed">
          <div className="space-y-2">
            <h4 className="font-bold text-white text-sm">SSRF & Safe Resource Restrictions</h4>
            <p>
              SourceVault enforces strict Server-Side Request Forgery (SSRF) controls. All DNS resolutions, IP addresses, and HTTP redirects are verified against private networks (RFC1918), loopbacks, link-local, and cloud metadata endpoints.
            </p>
            <p>
              Per-host concurrency is throttled, file sizes are restricted to 20MB per asset, and duplicate paths are deduplicated safely using cryptographic SHA-256 checksums.
            </p>
          </div>
          <div className="space-y-2">
            <h4 className="font-bold text-white text-sm">URL-Only Mode vs. Chrome Extension</h4>
            <p>
              In public URL-only mode, SourceVault crawls the public HTML and discovered assets without browser credentials. Virtual DevTools source trees and authenticated dashboards cannot be accessed in this mode.
            </p>
            <p>
              To inspect logged-in web applications or capture active session network payloads, connect the Manifest V3 Chrome Extension via the status badge in the header.
            </p>
          </div>
        </div>
      </section>
    </div>
  );
};

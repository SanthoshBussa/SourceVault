import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Globe,
  CheckCircle2,
  AlertTriangle,
  FileCode,
  Layers,
  HardDrive,
  Chrome,
  ArrowRight,
  ShieldAlert,
  Sliders,
  Sparkles,
  Loader2,
  ExternalLink
} from 'lucide-react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import { analyzeWebsite, createCollectionJob, formatBytes, getBrowserStatus } from '../api';

const COLORS = ['#4338ca', '#0284c7', '#059669', '#d97706', '#dc2626', '#8b5cf6', '#64748b'];

export const AnalysisPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const urlParam = searchParams.get('url') || '';
  const navigate = useNavigate();

  // Job options
  const [includeThirdParty, setIncludeThirdParty] = useState(false);
  const [reconstructSourceMaps, setReconstructSourceMaps] = useState(true);
  const [maxResourceCount, setMaxResourceCount] = useState(500);
  const [isStarting, setIsStarting] = useState(false);

  const { data: browserStatus } = useQuery({
    queryKey: ['browserStatus'],
    queryFn: getBrowserStatus
  });

  const { data: analysis, isLoading, error } = useQuery({
    queryKey: ['analysis', urlParam],
    queryFn: () => analyzeWebsite(urlParam, Boolean(browserStatus?.connected)),
    enabled: Boolean(urlParam),
    retry: 1
  });

  const handleStartCollection = async () => {
    if (!analysis) return;
    setIsStarting(true);
    try {
      const job = await createCollectionJob({
        analysisId: analysis.analysisId,
        websiteUrl: analysis.normalizedUrl,
        collectionMode: browserStatus?.connected ? 'BROWSER_INTEGRATED' : 'URL_ONLY',
        includeThirdParty,
        reconstructSourceMaps,
        maxResourceCount
      });
      navigate(`/collection/${job.id}`);
    } catch (err: any) {
      alert('Failed to start collection: ' + (err.message || 'Unknown error'));
      setIsStarting(false);
    }
  };

  if (!urlParam) {
    return (
      <div className="py-20 text-center space-y-4">
        <Globe className="w-12 h-12 mx-auto text-slate-300" />
        <h2 className="text-xl font-bold text-slate-800">No URL Provided</h2>
        <p className="text-sm text-slate-500">Please enter a valid website URL on the dashboard to run an analysis.</p>
        <Link to="/" className="inline-block px-4 py-2 bg-brand-700 text-white rounded-lg text-sm font-semibold">
          Return to Dashboard
        </Link>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="py-24 text-center space-y-4 animate-fadeIn">
        <Loader2 className="w-10 h-10 mx-auto text-brand-700 animate-spin" />
        <h2 className="text-xl font-bold text-slate-900">Inspecting {urlParam}...</h2>
        <p className="text-sm text-slate-500 max-w-md mx-auto">
          Validating protocol, verifying SSRF restrictions, downloading public HTML, and discovering scripts, styles, fonts, and source-map manifests.
        </p>
      </div>
    );
  }

  if (error || !analysis) {
    return (
      <div className="py-20 text-center space-y-4 max-w-lg mx-auto">
        <AlertTriangle className="w-12 h-12 mx-auto text-rose-500" />
        <h2 className="text-xl font-bold text-slate-900">Website Analysis Failed</h2>
        <p className="text-sm text-slate-600">
          {(error as Error)?.message || 'The website could not be inspected. Please verify the URL is public and reachable.'}
        </p>
        <Link to="/" className="inline-block px-5 py-2.5 bg-brand-700 text-white rounded-xl text-sm font-semibold hover:bg-brand-800 transition">
          Try Another URL
        </Link>
      </div>
    );
  }

  const chartData = Object.entries(analysis.typeBreakdown || {}).map(([name, value]) => ({
    name,
    value
  }));

  return (
    <div className="space-y-8 pb-16">
      {/* Website Header Card */}
      <section className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-8 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="flex items-start gap-4">
          <div className="w-14 h-14 rounded-2xl bg-slate-100 border border-slate-200 flex items-center justify-center shrink-0 overflow-hidden p-2">
            {analysis.faviconUrl ? (
              <img
                src={analysis.faviconUrl}
                alt="Favicon"
                className="w-full h-full object-contain"
                onError={(e) => {
                  (e.target as HTMLElement).style.display = 'none';
                }}
              />
            ) : (
              <Globe className="w-7 h-7 text-slate-400" />
            )}
          </div>
          <div className="space-y-1">
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-xl sm:text-2xl font-bold text-slate-900">{analysis.title || analysis.websiteOrigin}</h1>
              <span className={`px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                analysis.pageAccessible ? 'bg-emerald-50 text-emerald-800 border border-emerald-200' : 'bg-rose-50 text-rose-800'
              }`}>
                {analysis.pageAccessible ? `HTTP ${analysis.httpStatus} OK` : `HTTP ${analysis.httpStatus} Inaccessible`}
              </span>
            </div>
            <p className="text-xs text-slate-500 font-mono break-all flex items-center gap-1.5">
              <span>{analysis.normalizedUrl}</span>
              <a href={analysis.normalizedUrl} target="_blank" rel="noopener noreferrer" className="hover:text-brand-700">
                <ExternalLink className="w-3.5 h-3.5" />
              </a>
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={handleStartCollection}
            disabled={isStarting || !analysis.pageAccessible}
            className="w-full sm:w-auto px-6 py-3.5 bg-brand-700 hover:bg-brand-800 active:bg-brand-900 text-white font-semibold rounded-xl text-sm flex items-center justify-center gap-2 shadow-sm transition disabled:opacity-50 cursor-pointer"
          >
            <Sparkles className="w-4 h-4" />
            <span>{isStarting ? 'Preparing Job...' : 'Start Collection'}</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </section>

      {/* Discovery Summary Grid */}
      <section className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="p-5 bg-white rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-1">
            <span className="text-xs font-semibold uppercase tracking-wider">Discovered Assets</span>
            <Layers className="w-4 h-4 text-brand-600" />
          </div>
          <p className="text-2xl font-bold text-slate-900">{analysis.totalDiscovered}</p>
          <p className="text-xs text-slate-500 mt-1">Scripts, styles, images, fonts</p>
        </div>

        <div className="p-5 bg-white rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-1">
            <span className="text-xs font-semibold uppercase tracking-wider">Source Maps</span>
            <FileCode className="w-4 h-4 text-violet-600" />
          </div>
          <p className="text-2xl font-bold text-slate-900">{analysis.sourceMapCount}</p>
          <p className="text-xs text-slate-500 mt-1">Eligible for reconstruction</p>
        </div>

        <div className="p-5 bg-white rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-1">
            <span className="text-xs font-semibold uppercase tracking-wider">Estimated Size</span>
            <HardDrive className="w-4 h-4 text-emerald-600" />
          </div>
          <p className="text-2xl font-bold text-slate-900">{formatBytes(analysis.estimatedSizeBytes)}</p>
          <p className="text-xs text-slate-500 mt-1">Uncompressed payload</p>
        </div>

        <div className="p-5 bg-white rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-1">
            <span className="text-xs font-semibold uppercase tracking-wider">Collection Mode</span>
            <Chrome className="w-4 h-4 text-amber-500" />
          </div>
          <p className="text-base font-bold text-slate-900">
            {browserStatus?.connected ? 'Browser Integrated' : 'URL-Only Mode'}
          </p>
          <p className="text-xs text-slate-500 mt-1">
            {browserStatus?.connected ? 'Using extension session' : 'Public assets only'}
          </p>
        </div>
      </section>

      {/* Warnings Banner if any */}
      {analysis.warnings && analysis.warnings.length > 0 && (
        <section className="bg-amber-50/70 border border-amber-200 rounded-xl p-4 text-xs text-amber-900 space-y-1.5">
          <div className="flex items-center gap-1.5 font-bold">
            <ShieldAlert className="w-4 h-4 text-amber-700" />
            <span>Analysis Notices & Capabilities</span>
          </div>
          <ul className="list-disc pl-5 space-y-1 text-slate-700">
            {analysis.warnings.map((w, idx) => (
              <li key={idx}>{w}</li>
            ))}
          </ul>
        </section>
      )}

      {/* Two Column Section: Resource Breakdown & Job Configuration */}
      <section className="grid lg:grid-cols-3 gap-6">
        {/* Resource Breakdown Card */}
        <div className="lg:col-span-2 bg-white rounded-2xl border border-slate-200 p-6 shadow-xs space-y-6">
          <div className="flex items-center justify-between border-b border-slate-100 pb-4">
            <div>
              <h3 className="font-bold text-slate-900 text-base">Resource Categorization</h3>
              <p className="text-xs text-slate-500">Breakdown of discovered website components</p>
            </div>
            <span className="text-xs font-semibold px-2.5 py-1 bg-slate-100 rounded-lg text-slate-700">
              {analysis.totalDiscovered} Items
            </span>
          </div>

          <div className="grid sm:grid-cols-2 gap-6 items-center">
            <div className="h-48 flex items-center justify-center">
              {chartData.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={chartData}
                      dataKey="value"
                      nameKey="name"
                      cx="50%"
                      cy="50%"
                      innerRadius={45}
                      outerRadius={75}
                      paddingAngle={3}
                    >
                      {chartData.map((_, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <p className="text-xs text-slate-400">No chart data</p>
              )}
            </div>

            <div className="space-y-2 text-xs">
              {chartData.map((item, idx) => (
                <div key={item.name} className="flex items-center justify-between p-2 rounded-lg bg-slate-50">
                  <div className="flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: COLORS[idx % COLORS.length] }} />
                    <span className="font-medium text-slate-800">{item.name}</span>
                  </div>
                  <span className="font-bold text-slate-900">{item.value}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Sample Discovered Assets preview */}
          <div className="border-t border-slate-100 pt-4">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-2">Sample Discovered Assets</h4>
            <div className="max-h-48 overflow-y-auto space-y-1.5 text-xs">
              {analysis.discoveredResources?.slice(0, 10).map((r, i) => (
                <div key={i} className="flex items-center justify-between p-2 bg-slate-50 rounded-lg font-mono">
                  <span className="truncate max-w-sm text-slate-800" title={r.url}>{r.url}</span>
                  <span className="text-[10px] px-2 py-0.5 rounded bg-brand-50 text-brand-700 font-semibold shrink-0">
                    {r.resourceType}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Collection Options Card */}
        <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-xs space-y-5">
          <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
            <Sliders className="w-4 h-4 text-brand-700" />
            <h3 className="font-bold text-slate-900 text-sm">Collection Configuration</h3>
          </div>

          <div className="space-y-4 text-xs">
            {/* Option 1: Reconstruct Source Maps */}
            <label className="flex items-start gap-3 p-3 rounded-xl border border-slate-200 hover:bg-slate-50 cursor-pointer transition">
              <input
                type="checkbox"
                checked={reconstructSourceMaps}
                onChange={(e) => setReconstructSourceMaps(e.target.checked)}
                className="mt-0.5 rounded text-brand-700 focus:ring-brand-500"
              />
              <div className="space-y-0.5">
                <p className="font-bold text-slate-900">Reconstruct Source-Maps</p>
                <p className="text-slate-500">
                  Extract original source files when <code className="bg-slate-100 px-1 py-0.5 rounded">sourcesContent</code> is present in .map files.
                </p>
              </div>
            </label>

            {/* Option 2: Include Third Party */}
            <label className="flex items-start gap-3 p-3 rounded-xl border border-slate-200 hover:bg-slate-50 cursor-pointer transition">
              <input
                type="checkbox"
                checked={includeThirdParty}
                onChange={(e) => setIncludeThirdParty(e.target.checked)}
                className="mt-0.5 rounded text-brand-700 focus:ring-brand-500"
              />
              <div className="space-y-0.5">
                <p className="font-bold text-slate-900">Include Third-Party Domains</p>
                <p className="text-slate-500">
                  Download external CDNs, Google Fonts, and external scripts into separate host directories.
                </p>
              </div>
            </label>

            {/* Option 3: Resource Limit */}
            <div className="space-y-1.5 p-3 rounded-xl border border-slate-200">
              <div className="flex justify-between font-bold text-slate-900">
                <span>Maximum Resource Count</span>
                <span className="text-brand-700">{maxResourceCount} files</span>
              </div>
              <input
                type="range"
                min="50"
                max="2000"
                step="50"
                value={maxResourceCount}
                onChange={(e) => setMaxResourceCount(Number(e.target.value))}
                className="w-full accent-brand-700"
              />
              <p className="text-[11px] text-slate-400">Protects local storage and download bandwidth.</p>
            </div>
          </div>

          <button
            onClick={handleStartCollection}
            disabled={isStarting || !analysis.pageAccessible}
            className="w-full py-3 bg-brand-700 hover:bg-brand-800 text-white font-semibold rounded-xl text-xs flex items-center justify-center gap-2 shadow-sm transition disabled:opacity-50 cursor-pointer"
          >
            <span>Confirm & Start Collection</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>
      </section>
    </div>
  );
};

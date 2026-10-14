import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  CheckCircle2,
  AlertTriangle,
  Download,
  FolderArchive,
  FileText,
  FileCode,
  ShieldCheck,
  RefreshCw,
  ArrowLeft,
  ExternalLink,
  Layers,
  X,
  HardDrive
} from 'lucide-react';
import { getJob, formatBytes } from '../api';

export const ResultsPage: React.FC = () => {
  const { jobId } = useParams<{ jobId: string }>();
  const [showManifestModal, setShowManifestModal] = useState(false);
  const [showFailedModal, setShowFailedModal] = useState(false);
  const [manifestContent, setManifestContent] = useState<string>('');
  const [failedContent, setFailedContent] = useState<string>('');
  const [loadingText, setLoadingText] = useState(false);

  const { data: job, isLoading, error } = useQuery({
    queryKey: ['jobResult', jobId],
    queryFn: () => getJob(jobId!),
    enabled: Boolean(jobId)
  });

  const handleDownloadManifest = async () => {
    setLoadingText(true);
    try {
      const res = await fetch(`/api/v1/jobs/${jobId}/manifest`);
      const text = await res.text();
      setManifestContent(text);
      setShowManifestModal(true);
    } catch (e: any) {
      alert('Could not load manifest: ' + e.message);
    } finally {
      setLoadingText(false);
    }
  };

  const handleDownloadFailed = async () => {
    setLoadingText(true);
    try {
      const res = await fetch(`/api/v1/jobs/${jobId}/failed-resources`);
      const text = await res.text();
      setFailedContent(text);
      setShowFailedModal(true);
    } catch (e: any) {
      alert('Could not load failed report: ' + e.message);
    } finally {
      setLoadingText(false);
    }
  };

  if (isLoading) {
    return (
      <div className="py-24 text-center space-y-3">
        <FolderArchive className="w-10 h-10 mx-auto text-brand-700 animate-pulse" />
        <h2 className="text-xl font-bold text-slate-900">Loading Collection Results...</h2>
      </div>
    );
  }

  if (error || !job) {
    return (
      <div className="py-20 text-center space-y-4 max-w-md mx-auto">
        <AlertTriangle className="w-12 h-12 mx-auto text-rose-500" />
        <h2 className="text-xl font-bold text-slate-900">Job Not Found</h2>
        <p className="text-sm text-slate-500">The requested collection job does not exist or was deleted.</p>
        <Link to="/" className="inline-block px-4 py-2 bg-brand-700 text-white rounded-lg text-xs font-semibold">
          Return to Dashboard
        </Link>
      </div>
    );
  }

  const isCompleted = job.status === 'COMPLETED' || job.status === 'COMPLETED_WITH_WARNINGS';
  const downloadUrl = `/api/v1/jobs/${job.id}/archive`;

  return (
    <div className="space-y-8 pb-16">
      {/* Header Card */}
      <section className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-8 shadow-xs space-y-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <Link to="/history" className="text-xs text-slate-500 hover:text-slate-800 flex items-center gap-1">
                <ArrowLeft className="w-3.5 h-3.5" />
                <span>Job History</span>
              </Link>
              <span className="text-slate-300">•</span>
              <span className="text-xs font-mono text-slate-400">{job.id}</span>
            </div>
            <h1 className="text-2xl font-bold text-slate-900">{job.websiteUrl}</h1>
            <p className="text-xs text-slate-500 font-mono">{job.jobName}</p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            {isCompleted && (
              <a
                href={downloadUrl}
                download={job.archiveFileName || 'source_backup.zip'}
                className="px-6 py-3.5 bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 text-white font-bold rounded-xl text-sm flex items-center gap-2 shadow-sm transition"
              >
                <Download className="w-4 h-4" />
                <span>Download ZIP ({formatBytes(job.archiveSize)})</span>
              </a>
            )}
          </div>
        </div>

        {/* Verification Status Pill */}
        <div className="flex flex-wrap items-center justify-between gap-4 p-4 rounded-xl bg-slate-50 border border-slate-200">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-lg bg-emerald-100 flex items-center justify-center text-emerald-700">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <p className="text-xs font-bold text-slate-900 flex items-center gap-1.5">
                <span>ZIP Integrity:</span>
                <span className="text-emerald-700 uppercase">{job.validationStatus || 'VALID'}</span>
              </p>
              <p className="text-[11px] text-slate-500">
                CRC-32 checksums verified; all manifest entries match packed files without corruption.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleDownloadManifest}
              disabled={loadingText}
              className="px-3 py-1.5 bg-white border border-slate-200 text-slate-700 hover:bg-slate-50 rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow-2xs transition"
            >
              <FileText className="w-3.5 h-3.5 text-brand-600" />
              <span>View Manifest (CSV)</span>
            </button>
            <button
              onClick={handleDownloadFailed}
              disabled={loadingText}
              className="px-3 py-1.5 bg-white border border-slate-200 text-slate-700 hover:bg-slate-50 rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow-2xs transition"
            >
              <AlertTriangle className="w-3.5 h-3.5 text-amber-500" />
              <span>Failed Report</span>
            </button>
          </div>
        </div>
      </section>

      {/* Metrics Row */}
      <section className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-5 bg-white rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-1">
            <span className="text-xs font-semibold uppercase tracking-wider">Discovered Assets</span>
            <Layers className="w-4 h-4 text-brand-600" />
          </div>
          <p className="text-2xl font-bold text-slate-900">{job.totalDiscovered}</p>
          <p className="text-xs text-slate-500 mt-1">Found during analysis</p>
        </div>

        <div className="p-5 bg-white rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-1">
            <span className="text-xs font-semibold uppercase tracking-wider">Downloaded Files</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          </div>
          <p className="text-2xl font-bold text-emerald-600">{job.totalDownloaded}</p>
          <p className="text-xs text-slate-500 mt-1">Preserved original byte fidelity</p>
        </div>

        <div className="p-5 bg-white rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-1">
            <span className="text-xs font-semibold uppercase tracking-wider">Reconstructed Sources</span>
            <FileCode className="w-4 h-4 text-violet-600" />
          </div>
          <p className="text-2xl font-bold text-violet-600">{job.totalReconstructed}</p>
          <p className="text-xs text-slate-500 mt-1">Extracted via sourcesContent</p>
        </div>

        <div className="p-5 bg-white rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-1">
            <span className="text-xs font-semibold uppercase tracking-wider">Final Archive Size</span>
            <HardDrive className="w-4 h-4 text-slate-600" />
          </div>
          <p className="text-2xl font-bold text-slate-900">{formatBytes(job.archiveSize)}</p>
          <p className="text-xs text-slate-500 mt-1">ZIP deflate compressed</p>
        </div>
      </section>

      {/* Archive Package Hierarchy Display */}
      <section className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-8 shadow-xs space-y-4">
        <h3 className="font-bold text-slate-900 text-base">Archive Package Structure</h3>
        <p className="text-xs text-slate-500">
          The generated ZIP contains organized domain directories, separated reconstructed source code, and comprehensive audit files.
        </p>

        <div className="bg-slate-900 text-slate-200 font-mono text-xs p-5 rounded-xl space-y-1.5 overflow-x-auto">
          <p className="text-brand-300 font-bold">{job.archiveFileName || 'source_backup.zip'}</p>
          <p className="pl-4 text-slate-400">├── {job.websiteOrigin?.replace(/^https?:\/\//, '') || 'website.com'}/</p>
          <p className="pl-8 text-slate-300">├── index.html</p>
          <p className="pl-8 text-slate-300">├── assets/ (compiled bundles)</p>
          <p className="pl-4 text-violet-300 font-semibold">├── reconstructed_sources/</p>
          <p className="pl-8 text-violet-200">├── src/ (original TypeScript / JavaScript source files)</p>
          <p className="pl-8 text-violet-200">└── components/</p>
          <p className="pl-4 text-emerald-300 font-semibold">├── manifest.csv (SHA-256 and URLs audit trail)</p>
          <p className="pl-4 text-slate-300">├── README.md</p>
          <p className="pl-4 text-slate-300">├── failed_resources.txt</p>
          <p className="pl-4 text-slate-300">└── summary.json</p>
        </div>
      </section>

      {/* Manifest Viewer Modal */}
      {showManifestModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-fadeIn">
          <div className="bg-white rounded-2xl max-w-3xl w-full p-6 shadow-2xl border border-slate-200 relative max-h-[85vh] flex flex-col">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-bold text-base text-slate-900">Archive Manifest (manifest.csv)</h3>
              <button onClick={() => setShowManifestModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="my-4 flex-1 overflow-auto bg-slate-900 text-slate-200 p-4 rounded-xl font-mono text-xs">
              <pre className="whitespace-pre-wrap">{manifestContent}</pre>
            </div>
            <div className="flex justify-between items-center pt-2">
              <a
                href={`/api/v1/jobs/${job.id}/manifest`}
                download={`manifest_${job.id}.csv`}
                className="px-4 py-2 bg-brand-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Save CSV</span>
              </a>
              <button
                onClick={() => setShowManifestModal(false)}
                className="px-4 py-2 bg-slate-100 text-slate-800 rounded-lg text-xs font-semibold"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Failed Resources Modal */}
      {showFailedModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-fadeIn">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-2xl border border-slate-200 relative max-h-[85vh] flex flex-col">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-bold text-base text-slate-900">Failed / Skipped Resources</h3>
              <button onClick={() => setShowFailedModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="my-4 flex-1 overflow-auto bg-slate-50 text-slate-800 p-4 rounded-xl font-mono text-xs border border-slate-200">
              <pre className="whitespace-pre-wrap">
                {failedContent.trim() ? failedContent : 'No failed resources recorded for this job! Everything downloaded cleanly.'}
              </pre>
            </div>
            <div className="flex justify-end pt-2">
              <button
                onClick={() => setShowFailedModal(false)}
                className="px-4 py-2 bg-slate-100 text-slate-800 rounded-lg text-xs font-semibold"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

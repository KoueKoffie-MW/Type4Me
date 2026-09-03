import React, { useState, useMemo } from 'react';
import { Mic, CheckCircle2, AlertTriangle, ArrowRight, Save, Sparkles, Volume2, RotateCcw, Cpu } from 'lucide-react';
import { CALIBRATION_PASSAGES, CalibrationPassage } from '../engine/accent/CalibrationPassages';
import { NeedlemanWunschAligner, AlignmentReport } from '../engine/accent/NeedlemanWunsch';
import { ConfusionMatrixBuilder, UserAccentProfile } from '../engine/accent/ConfusionMatrix';

interface AccentCalibrationStudioProps {
  activeProfile: UserAccentProfile;
  onSaveProfile: (profile: UserAccentProfile) => Promise<void>;
}

export const AccentCalibrationStudio: React.FC<AccentCalibrationStudioProps> = ({
  activeProfile,
  onSaveProfile,
}) => {
  const [selectedPassage, setSelectedPassage] = useState<CalibrationPassage>(CALIBRATION_PASSAGES[0]);
  const [spokenTranscript, setSpokenTranscript] = useState('');
  const [isSaved, setIsSaved] = useState(false);

  // Compute live alignment
  const alignmentReport: AlignmentReport | null = useMemo(() => {
    if (!spokenTranscript.trim()) return null;
    return NeedlemanWunschAligner.align(selectedPassage.text, spokenTranscript);
  }, [selectedPassage, spokenTranscript]);

  // Compute discovered substitutions
  const discoveredSubstitutions = useMemo(() => {
    if (!alignmentReport) return [];
    return ConfusionMatrixBuilder.buildSubstitutions(alignmentReport.tokens);
  }, [alignmentReport]);

  const handleSaveToProfile = async () => {
    if (!alignmentReport) return;
    const updated = ConfusionMatrixBuilder.updateProfile(
      activeProfile,
      discoveredSubstitutions,
      alignmentReport.wordErrorRate,
      alignmentReport.totalReferenceWords
    );
    await onSaveProfile(updated);
    setIsSaved(true);
    setTimeout(() => setIsSaved(false), 3000);
  };

  const handleSimulateAfrikaansReading = () => {
    // Quick demonstration simulator of typical South African / Afrikaans accent speech recognition
    if (selectedPassage.id === 'passage_engineering_core') {
      setSpokenTranscript(
        'Please inspect the sim you link model and ensure the state flow chart correctly handles wariable water neon rotation vectors. We must verify that the bowling flag on the disk bort does not report a false foult before writing the recort to disk.'
      );
    } else if (selectedPassage.id === 'passage_fricatives_plosives') {
      setSpokenTranscript(
        'Thirty-three engineers thought that the third buck was caused by a dead-lock in the mewtex thread. When the second test failed, they sent a patch to prevent the bat tack from corrupting the sent buffer in the repo.'
      );
    } else {
      setSpokenTranscript(
        'Generate a jerkin test scenario covering all edge cases. Read the active transcript in jsonl format, extract the ast node descriptors, and apply a surgical diff without breaking any invariant constraints or existing comments.'
      );
    }
  };

  return (
    <div className="flex flex-col gap-6 max-w-5xl mx-auto p-4 select-text">
      {/* Header Banner */}
      <div className="flex items-start justify-between bg-slate-900/90 border border-teal-800/40 rounded-xl p-5 shadow-lg">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Cpu className="w-5 h-5 text-teal-400" />
            <h2 className="text-lg font-bold text-slate-100">"Learn-My-Accent" Calibration Studio</h2>
            <span className="text-xs bg-teal-950 text-teal-300 border border-teal-800 px-2 py-0.5 rounded font-mono">
              Active: {activeProfile.name}
            </span>
          </div>
          <p className="text-xs text-slate-400 max-w-2xl leading-relaxed">
            Standard speech recognition models misinterpret regional pronunciations and engineer slang. Read the diagnostic teleprompter passage aloud below. We compute your phonetic confusion matrix and calibrate the repair engine specifically to your voice.
          </p>
        </div>

        <div className="flex flex-col items-end gap-1 font-mono text-xs text-slate-400">
          <span>Completed Passages: <strong className="text-teal-400">{activeProfile.stats.calibrationPassagesCompleted}</strong></span>
          <span>Calibrated Rules: <strong className="text-teal-400">{activeProfile.detailedRules.length}</strong></span>
          <span>Avg WER: <strong className="text-amber-400">{Math.round((activeProfile.stats.averageWer || 0) * 100)}%</strong></span>
        </div>
      </div>

      {/* Passage Selector */}
      <div className="flex items-center gap-2">
        <span className="text-xs font-mono text-slate-400">Select Passage:</span>
        {CALIBRATION_PASSAGES.map((p) => (
          <button
            key={p.id}
            onClick={() => {
              setSelectedPassage(p);
              setSpokenTranscript('');
            }}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
              selectedPassage.id === p.id
                ? 'bg-teal-600 text-white shadow-md shadow-teal-900/40'
                : 'bg-slate-900 border border-slate-800 text-slate-400 hover:text-slate-200'
            }`}
          >
            {p.title.split(':')[0]}
          </button>
        ))}
      </div>

      {/* Teleprompter Box */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 flex flex-col gap-3">
        <div className="flex items-center justify-between text-xs font-mono text-slate-400 pb-2 border-b border-slate-800">
          <span className="flex items-center gap-2">
            <Volume2 className="w-4 h-4 text-teal-400" />
            <strong className="text-slate-200">{selectedPassage.title}</strong>
          </span>
          <span className="text-[11px] text-slate-500">
            Target Features: {selectedPassage.targetFeatures.join(' • ')}
          </span>
        </div>

        <p className="text-base text-slate-100 font-serif leading-loose tracking-wide p-3 bg-slate-950/80 rounded-lg border border-slate-800">
          "{selectedPassage.text}"
        </p>

        {/* Input Area */}
        <div className="flex flex-col gap-2 mt-2">
          <div className="flex items-center justify-between text-xs font-mono text-slate-400">
            <span className="flex items-center gap-1.5">
              <Mic className="w-3.5 h-3.5 text-amber-400" />
              Spoken Transcript (Use <strong className="text-teal-300">Win+H</strong> to dictate while focused):
            </span>
            <div className="flex items-center gap-2">
              <button
                onClick={handleSimulateAfrikaansReading}
                className="text-[11px] bg-slate-800 hover:bg-slate-700 text-teal-300 px-2 py-0.5 rounded transition-colors flex items-center gap-1"
                title="Test with typical simulated South African accent transcript"
              >
                <Sparkles className="w-3 h-3" />
                Simulate Accent Input
              </button>
              {spokenTranscript && (
                <button
                  onClick={() => setSpokenTranscript('')}
                  className="text-[11px] text-slate-500 hover:text-slate-300 transition-colors flex items-center gap-1"
                >
                  <RotateCcw className="w-3 h-3" />
                  Clear
                </button>
              )}
            </div>
          </div>

          <textarea
            value={spokenTranscript}
            onChange={(e) => setSpokenTranscript(e.target.value)}
            placeholder="Focus here and press Win+H, then read the text above aloud..."
            rows={3}
            className="w-full bg-slate-950 border border-slate-800 focus:border-teal-600 rounded-lg p-3 text-sm font-sans text-slate-200 placeholder-slate-600 outline-none leading-relaxed"
          />
        </div>
      </div>

      {/* Alignment Visualizer & Metrics */}
      {alignmentReport && (
        <div className="flex flex-col gap-4 bg-slate-900/90 border border-slate-800 rounded-xl p-5">
          {/* Metrics Scorecard */}
          <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
            <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 flex flex-col">
              <span className="text-[11px] font-mono text-slate-400">Phonetic Accuracy</span>
              <strong className="text-xl font-bold text-teal-400">{alignmentReport.accuracyPercentage}%</strong>
            </div>
            <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 flex flex-col">
              <span className="text-[11px] font-mono text-slate-400">Word Error Rate</span>
              <strong className="text-xl font-bold text-amber-400">{alignmentReport.wordErrorRate}</strong>
            </div>
            <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 flex flex-col">
              <span className="text-[11px] font-mono text-slate-400">Substitutions</span>
              <strong className="text-xl font-bold text-slate-200">{alignmentReport.substitutions}</strong>
            </div>
            <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 flex flex-col">
              <span className="text-[11px] font-mono text-slate-400">Phonetic Homophones</span>
              <strong className="text-xl font-bold text-blue-400">{alignmentReport.phoneticMatchesCount}</strong>
            </div>
            <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 flex flex-col">
              <span className="text-[11px] font-mono text-slate-400">Deletions / Insertions</span>
              <strong className="text-xl font-bold text-slate-400">{alignmentReport.deletions} / {alignmentReport.insertions}</strong>
            </div>
          </div>

          {/* Color Token Alignment Flow */}
          <div className="mt-2">
            <span className="text-xs font-mono text-slate-400 block mb-2">Phonetic Alignment Analysis:</span>
            <div className="flex flex-wrap gap-1.5 p-4 bg-slate-950 rounded-lg border border-slate-800/80 leading-relaxed font-mono text-xs">
              {alignmentReport.tokens.map((token, idx) => {
                if (token.type === 'MATCH') {
                  return (
                    <span key={idx} className="bg-emerald-950/70 text-emerald-300 border border-emerald-800/60 px-2 py-1 rounded">
                      {token.referenceWord}
                    </span>
                  );
                } else if (token.type === 'SUBSTITUTION') {
                  return (
                    <span
                      key={idx}
                      className={`px-2 py-1 rounded border flex items-center gap-1 ${
                        token.phoneticMatch
                          ? 'bg-amber-950/80 text-amber-300 border-amber-800/80'
                          : 'bg-rose-950/80 text-rose-300 border-rose-800/80'
                      }`}
                      title={token.phoneticMatch ? 'Phonetic homophone detected' : 'Acoustic substitution'}
                    >
                      <span className="line-through opacity-60">{token.spokenWord}</span>
                      <ArrowRight className="w-2.5 h-2.5" />
                      <strong>{token.referenceWord}</strong>
                    </span>
                  );
                } else if (token.type === 'DELETION') {
                  return (
                    <span key={idx} className="bg-red-950/60 text-red-400 border border-red-900/60 px-2 py-1 rounded line-through">
                      {token.referenceWord}
                    </span>
                  );
                } else {
                  return (
                    <span key={idx} className="bg-purple-950/60 text-purple-300 border border-purple-900/60 px-2 py-1 rounded">
                      +{token.spokenWord}
                    </span>
                  );
                }
              })}
            </div>
          </div>

          {/* Discovered Substitutions Table */}
          {discoveredSubstitutions.length > 0 && (
            <div className="mt-2">
              <span className="text-xs font-mono text-slate-400 block mb-2">
                Learned Pronunciation Substitutions for Your Accent Profile:
              </span>
              <div className="overflow-x-auto">
                <table className="w-full text-left font-mono text-xs border border-slate-800 rounded-lg overflow-hidden">
                  <thead className="bg-slate-950 text-slate-400 border-b border-slate-800">
                    <tr>
                      <th className="p-2.5">ASR Output (Spoken)</th>
                      <th className="p-2.5">Intended Meaning</th>
                      <th className="p-2.5">Type</th>
                      <th className="p-2.5">Confidence</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800 bg-slate-950/60">
                    {discoveredSubstitutions.map((sub, idx) => (
                      <tr key={idx} className="hover:bg-slate-900/60">
                        <td className="p-2.5 text-rose-300 font-semibold">{sub.misrecognized}</td>
                        <td className="p-2.5 text-teal-300 font-semibold">{sub.intended}</td>
                        <td className="p-2.5 text-slate-400">
                          {sub.phoneticKeyMatch ? (
                            <span className="text-[10px] bg-teal-950 text-teal-300 border border-teal-800 px-1.5 py-0.5 rounded">
                              Phonetic Shift
                            </span>
                          ) : (
                            <span className="text-[10px] bg-slate-800 text-slate-400 px-1.5 py-0.5 rounded">
                              Acoustic Slip
                            </span>
                          )}
                        </td>
                        <td className="p-2.5 text-slate-300">{Math.round(sub.confidenceScore * 100)}%</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Save Profile Button */}
          <div className="flex items-center justify-end mt-3 pt-3 border-t border-slate-800">
            <button
              onClick={handleSaveToProfile}
              className="px-4 py-2 bg-teal-600 hover:bg-teal-500 text-white font-medium rounded-lg text-xs flex items-center gap-2 shadow-lg shadow-teal-900/40 transition-all active:scale-95"
            >
              {isSaved ? (
                <>
                  <CheckCircle2 className="w-4 h-4 text-white" />
                  Accent Profile Calibrated & Saved!
                </>
              ) : (
                <>
                  <Save className="w-4 h-4" />
                  Save to My Accent Profile
                </>
              )}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

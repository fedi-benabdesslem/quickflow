import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { CreateMeetingTemplateRequest, MeetingTemplateData } from '../../types/template';
import { StructuredModeData } from '../../types';

interface SaveTemplateModalProps {
    isOpen: boolean;
    onClose: () => void;
    currentFormData: StructuredModeData;
    onSave: (request: CreateMeetingTemplateRequest) => Promise<void>;
}

export default function SaveTemplateModal({ isOpen, onClose, currentFormData, onSave }: SaveTemplateModalProps) {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [loading, setLoading] = useState(false);

    // Selection state for what to include
    const [includeTitle, setIncludeTitle] = useState(true);
    const [includeParticipants, setIncludeParticipants] = useState(true);
    const [includeAgenda, setIncludeAgenda] = useState(true);
    const [includeLocation, setIncludeLocation] = useState(true);
    const [includeDuration, setIncludeDuration] = useState(true);
    const [includePreferences, setIncludePreferences] = useState(true);
    const [includeOrganizer, setIncludeOrganizer] = useState(true);

    const handleSubmit = async () => {
        if (!name.trim()) return;

        setLoading(true);
        try {
            // Calculate duration
            let defaultDuration = 60;
            if (currentFormData.meetingInfo.startTime && currentFormData.meetingInfo.endTime) {
                const start = new Date(`2000-01-01T${currentFormData.meetingInfo.startTime}`);
                const end = new Date(`2000-01-01T${currentFormData.meetingInfo.endTime}`);
                defaultDuration = Math.round((end.getTime() - start.getTime()) / 60000);
            }

            // Construct template data based on selections
            const templateData: MeetingTemplateData = {
                meetingInfo: {
                    title: includeTitle ? currentFormData.meetingInfo.title : '',
                    defaultDuration: includeDuration ? defaultDuration : 60,
                    defaultStartTime: includeDuration ? currentFormData.meetingInfo.startTime : '10:00',
                    location: includeLocation ? currentFormData.meetingInfo.location : '',
                    organizer: includeOrganizer ? currentFormData.meetingInfo.organizer : undefined
                },
                participants: includeParticipants ? currentFormData.participants.map(p => ({
                    name: p.name,
                    email: p.email,
                    role: p.role
                })) : [],
                agendaStructure: includeAgenda ? currentFormData.agenda.map(a => ({
                    title: a.title,
                    objective: a.objective
                })) : [],
                outputPreferences: includePreferences ? currentFormData.outputPreferences : {
                    tone: 'Formal', length: 'Standard', includeSections: {
                        attendees: true, agenda: true, decisions: true, actionItems: true, additionalNotes: true
                    }, pdfFooter: 'None'
                }
            };

            const request: CreateMeetingTemplateRequest = {
                name: name.trim(),
                description: description.trim(),
                templateData
            };

            await onSave(request);
            onClose();
        } catch (error) {
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    return (
        <AnimatePresence>
            {isOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
                    <motion.div
                        initial={{ opacity: 0, scale: 0.95 }}
                        animate={{ opacity: 1, scale: 1 }}
                        exit={{ opacity: 0, scale: 0.95 }}
                        className="glass-card w-full max-w-lg overflow-hidden"
                    >
                        <div className="p-6">
                            <h2 className="text-xl font-bold text-white mb-4">Save as Template</h2>

                            <div className="space-y-4 mb-6">
                                <div>
                                    <label className="block text-sm font-medium text-slate-300 mb-1">
                                        Template Name <span className="text-red-400">*</span>
                                    </label>
                                    <input
                                        type="text"
                                        value={name}
                                        onChange={(e) => setName(e.target.value)}
                                        placeholder="e.g., Weekly Team Sync"
                                        className="input-nebula w-full"
                                        autoFocus
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-slate-300 mb-1">
                                        Description (optional)
                                    </label>
                                    <textarea
                                        value={description}
                                        onChange={(e) => setDescription(e.target.value)}
                                        placeholder="What is this meeting template for?"
                                        className="input-nebula w-full h-20 resize-none"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-slate-300 mb-2">
                                        Include in Template:
                                    </label>
                                    <div className="grid grid-cols-2 gap-2">
                                        <label className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-slate-800/50">
                                            <input type="checkbox" checked={includeTitle} onChange={e => setIncludeTitle(e.target.checked)} className="rounded bg-slate-700 border-slate-600 text-blue-500" />
                                            <span className="text-sm text-slate-300">Meeting Title</span>
                                        </label>
                                        <label className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-slate-800/50">
                                            <input type="checkbox" checked={includeParticipants} onChange={e => setIncludeParticipants(e.target.checked)} className="rounded bg-slate-700 border-slate-600 text-blue-500" />
                                            <span className="text-sm text-slate-300">Participants ({currentFormData.participants.length})</span>
                                        </label>
                                        <label className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-slate-800/50">
                                            <input type="checkbox" checked={includeAgenda} onChange={e => setIncludeAgenda(e.target.checked)} className="rounded bg-slate-700 border-slate-600 text-blue-500" />
                                            <span className="text-sm text-slate-300">Agenda Structure ({currentFormData.agenda.length})</span>
                                        </label>
                                        <label className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-slate-800/50">
                                            <input type="checkbox" checked={includeLocation} onChange={e => setIncludeLocation(e.target.checked)} className="rounded bg-slate-700 border-slate-600 text-blue-500" />
                                            <span className="text-sm text-slate-300">Location</span>
                                        </label>
                                        <label className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-slate-800/50">
                                            <input type="checkbox" checked={includeDuration} onChange={e => setIncludeDuration(e.target.checked)} className="rounded bg-slate-700 border-slate-600 text-blue-500" />
                                            <span className="text-sm text-slate-300">Duration & Time</span>
                                        </label>
                                        <label className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-slate-800/50">
                                            <input type="checkbox" checked={includePreferences} onChange={e => setIncludePreferences(e.target.checked)} className="rounded bg-slate-700 border-slate-600 text-blue-500" />
                                            <span className="text-sm text-slate-300">Output Preferences</span>
                                        </label>
                                        <label className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-slate-800/50">
                                            <input type="checkbox" checked={includeOrganizer} onChange={e => setIncludeOrganizer(e.target.checked)} className="rounded bg-slate-700 border-slate-600 text-blue-500" />
                                            <span className="text-sm text-slate-300">Organizer</span>
                                        </label>
                                    </div>
                                </div>
                            </div>

                            <div className="flex justify-end gap-3">
                                <button
                                    onClick={onClose}
                                    className="px-4 py-2 text-sm text-slate-400 hover:text-white transition-colors"
                                >
                                    Cancel
                                </button>
                                <button
                                    onClick={handleSubmit}
                                    disabled={!name.trim() || loading}
                                    className="btn-primary py-2 px-6 disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    {loading ? 'Saving...' : 'Save Template'}
                                </button>
                            </div>
                        </div>
                    </motion.div>
                </div>
            )}
        </AnimatePresence>
    );
}

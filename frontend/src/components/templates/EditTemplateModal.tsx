import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { UpdateMeetingTemplateRequest, MeetingTemplate, MeetingTemplateData } from '../../types/template';

interface EditTemplateModalProps {
    isOpen: boolean;
    onClose: () => void;
    template: MeetingTemplate;
    onUpdate: (id: string, request: UpdateMeetingTemplateRequest) => Promise<void>;
}

export default function EditTemplateModal({ isOpen, onClose, template, onUpdate }: EditTemplateModalProps) {
    const [name, setName] = useState(template.name);
    const [description, setDescription] = useState(template.description || '');
    const [data, setData] = useState<MeetingTemplateData>(template.templateData);
    const [loading, setLoading] = useState(false);

    // Reset state when template changes
    useEffect(() => {
        if (isOpen) {
            setName(template.name);
            setDescription(template.description || '');
            setData(template.templateData);
        }
    }, [isOpen, template]);

    const handleSubmit = async () => {
        if (!name.trim()) return;

        setLoading(true);
        try {
            const request: UpdateMeetingTemplateRequest = {
                name: name.trim(),
                description: description.trim(),
                templateData: data
            };

            await onUpdate(template.id, request);
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
                        className="glass-card w-full max-w-lg h-[80vh] flex flex-col overflow-hidden"
                    >
                        <div className="p-6 border-b border-slate-700/50">
                            <h2 className="text-xl font-bold text-white">Edit Template</h2>
                        </div>

                        <div className="p-6 overflow-y-auto flex-1 space-y-6">
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-1">
                                    Template Name <span className="text-red-400">*</span>
                                </label>
                                <input
                                    type="text"
                                    value={name}
                                    onChange={(e) => setName(e.target.value)}
                                    className="input-nebula w-full"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-1">
                                    Description
                                </label>
                                <textarea
                                    value={description}
                                    onChange={(e) => setDescription(e.target.value)}
                                    className="input-nebula w-full h-20 resize-none"
                                />
                            </div>

                            <div className="border border-slate-700/50 rounded-lg p-4 bg-slate-900/30">
                                <h3 className="text-sm font-medium text-white mb-3">Default Values</h3>

                                <div className="space-y-4">
                                    <div>
                                        <label className="block text-xs font-medium text-slate-400 mb-1">Default Meeting Title</label>
                                        <input
                                            type="text"
                                            value={data.meetingInfo.title}
                                            onChange={(e) => setData(prev => ({
                                                ...prev,
                                                meetingInfo: { ...prev.meetingInfo, title: e.target.value }
                                            }))}
                                            className="input-nebula w-full text-sm py-1.5"
                                        />
                                    </div>
                                    <div className="grid grid-cols-2 gap-3">
                                        <div>
                                            <label className="block text-xs font-medium text-slate-400 mb-1">Duration (mins)</label>
                                            <input
                                                type="number"
                                                value={data.meetingInfo.defaultDuration}
                                                onChange={(e) => setData(prev => ({
                                                    ...prev,
                                                    meetingInfo: { ...prev.meetingInfo, defaultDuration: parseInt(e.target.value) || 60 }
                                                }))}
                                                className="input-nebula w-full text-sm py-1.5"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-xs font-medium text-slate-400 mb-1">Default Time</label>
                                            <input
                                                type="time"
                                                value={data.meetingInfo.defaultStartTime}
                                                onChange={(e) => setData(prev => ({
                                                    ...prev,
                                                    meetingInfo: { ...prev.meetingInfo, defaultStartTime: e.target.value }
                                                }))}
                                                className="input-nebula w-full text-sm py-1.5"
                                            />
                                        </div>
                                    </div>

                                    <div>
                                        <label className="block text-xs font-medium text-slate-400 mb-1">Participants ({data.participants.length})</label>
                                        <div className="flex flex-wrap gap-2 mb-2">
                                            {data.participants.map((p, idx) => (
                                                <span key={idx} className="bg-slate-700 text-slate-300 px-2 py-1 rounded text-xs flex items-center gap-1">
                                                    {p.name}
                                                    <button onClick={() => setData(prev => ({
                                                        ...prev,
                                                        participants: prev.participants.filter((_, i) => i !== idx)
                                                    }))} className="text-slate-400 hover:text-white">×</button>
                                                </span>
                                            ))}
                                        </div>
                                        {/* Simplified editing - full editing would require sub-forms */}
                                        <p className="text-xs text-slate-500 italic">Advanced participant editing available in Structured Form</p>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="p-6 border-t border-slate-700/50 flex justify-end gap-3 bg-slate-900/50">
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
                                {loading ? 'Saving...' : 'Save Changes'}
                            </button>
                        </div>
                    </motion.div>
                </div>
            )}
        </AnimatePresence>
    );
}

interface ParticipantTagProps {
    name: string
    role?: string
    onRemove: () => void
    showRole?: boolean
    onRoleChange?: (role: string) => void
    isAbsent?: boolean
}

const ROLE_OPTIONS = ['Chair', 'Secretary', 'Member', 'Guest']

export default function ParticipantTag({
    name,
    role,
    onRemove,
    showRole = false,
    onRoleChange,
    isAbsent = false
}: ParticipantTagProps) {
    return (
        <div
            className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-sm ${isAbsent
                ? 'bg-slate-700/50 text-slate-400'
                : 'bg-blue-600/20 text-blue-300 border border-blue-500/30'
                }`}
        >
            <span className="font-medium">{name}</span>
            {showRole && onRoleChange && (
                <select
                    value={role || ''}
                    onChange={(e) => onRoleChange(e.target.value)}
                    className="bg-transparent text-xs text-slate-400 border-none outline-none cursor-pointer"
                    onClick={(e) => e.stopPropagation()}
                >
                    <option value="">No role</option>
                    {ROLE_OPTIONS.map(r => (
                        <option key={r} value={r}>{r}</option>
                    ))}
                </select>
            )}
            {role && !onRoleChange && (
                <span className="text-xs text-slate-400">({role})</span>
            )}
            <button
                onClick={(e) => { e.stopPropagation(); onRemove() }}
                className="text-slate-400 hover:text-red-400 transition-colors ml-1"
            >
                ×
            </button>
        </div>
    )
}

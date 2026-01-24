interface FormProgressProps {
    completed: number
    total: number
}

export default function FormProgress({ completed, total }: FormProgressProps) {
    const percentage = Math.round((completed / total) * 100)

    return (
        <div className="mb-6">
            <div className="flex justify-between items-center mb-2">
                <span className="text-sm text-slate-400">Form Progress</span>
                <span className="text-sm text-slate-300">
                    <span className="text-blue-400 font-medium">{completed}</span> of{' '}
                    <span className="font-medium">{total}</span> required fields completed
                </span>
            </div>
            <div className="h-2 bg-slate-800 rounded-full overflow-hidden">
                <div
                    className={`h-full rounded-full transition-all duration-500 ${percentage === 100
                        ? 'bg-gradient-to-r from-green-500 to-emerald-400'
                        : 'bg-gradient-to-r from-blue-600 to-purple-500'
                        }`}
                    style={{ width: `${percentage}%` }}
                />
            </div>
        </div>
    )
}

import ReactQuill from 'react-quill'
import 'react-quill/dist/quill.snow.css'

interface RichTextEditorProps {
    value: string
    onChange: (value: string) => void
    placeholder?: string
    disabled?: boolean
}

// Quill toolbar configuration
const modules = {
    toolbar: [
        [{ header: [1, 2, 3, false] }],
        ['bold', 'italic', 'underline'],
        [{ list: 'ordered' }, { list: 'bullet' }],
        ['clean'],
    ],
}

const formats = ['header', 'bold', 'italic', 'underline', 'list', 'bullet']


/**
 * Rich text editor component using React Quill.
 * Styled to match the nebula theme.
 * Supports table rendering from LLM Markdown output.
 */
export default function RichTextEditor({
    value,
    onChange,
    placeholder = 'Start writing...',
    disabled = false,
}: RichTextEditorProps) {
    return (
        <div className={`rich-text-editor ${disabled ? 'opacity-50 pointer-events-none' : ''}`}>
            <ReactQuill
                theme="snow"
                value={value}
                onChange={onChange}
                modules={modules}
                formats={formats}
                placeholder={placeholder}
                readOnly={disabled}
            />
            <style>{`
                .rich-text-editor .ql-container {
                    background: rgba(15, 23, 42, 0.8);
                    border: 1px solid rgba(148, 163, 184, 0.2);
                    border-top: none;
                    border-radius: 0 0 0.75rem 0.75rem;
                    min-height: 200px;
                    font-family: inherit;
                    font-size: 1rem;
                }

                .rich-text-editor .ql-toolbar {
                    background: rgba(30, 41, 59, 0.9);
                    border: 1px solid rgba(148, 163, 184, 0.2);
                    border-radius: 0.75rem 0.75rem 0 0;
                }

                .rich-text-editor .ql-toolbar .ql-stroke {
                    stroke: #94a3b8;
                }

                .rich-text-editor .ql-toolbar .ql-fill {
                    fill: #94a3b8;
                }

                .rich-text-editor .ql-toolbar .ql-picker {
                    color: #94a3b8;
                }

                .rich-text-editor .ql-toolbar button:hover .ql-stroke,
                .rich-text-editor .ql-toolbar button.ql-active .ql-stroke {
                    stroke: #a855f7;
                }

                .rich-text-editor .ql-toolbar button:hover .ql-fill,
                .rich-text-editor .ql-toolbar button.ql-active .ql-fill {
                    fill: #a855f7;
                }

                .rich-text-editor .ql-editor {
                    color: #e2e8f0;
                    min-height: 200px;
                }

                .rich-text-editor .ql-editor.ql-blank::before {
                    color: #64748b;
                    font-style: normal;
                }

                .rich-text-editor .ql-editor h1 {
                    color: #f1f5f9;
                    font-size: 1.5rem;
                }

                .rich-text-editor .ql-editor h2 {
                    color: #f1f5f9;
                    font-size: 1.25rem;
                }

                .rich-text-editor .ql-editor h3 {
                    color: #f1f5f9;
                    font-size: 1.1rem;
                }

                .rich-text-editor .ql-toolbar .ql-picker-options {
                    background: rgba(30, 41, 59, 0.95);
                    border-color: rgba(148, 163, 184, 0.2);
                }

                .rich-text-editor .ql-toolbar .ql-picker-item {
                    color: #94a3b8;
                }

                .rich-text-editor .ql-toolbar .ql-picker-item:hover {
                    color: #a855f7;
                }

                /* Table styling for LLM-generated tables */
                .rich-text-editor .ql-editor table {
                    width: 100%;
                    border-collapse: collapse;
                    margin: 12px 0;
                    font-size: 0.9rem;
                }

                .rich-text-editor .ql-editor th,
                .rich-text-editor .ql-editor td {
                    border: 1px solid rgba(148, 163, 184, 0.25);
                    padding: 8px 12px;
                    text-align: left;
                }

                .rich-text-editor .ql-editor th {
                    background: rgba(99, 102, 241, 0.15);
                    color: #c7d2fe;
                    font-weight: 600;
                }

                .rich-text-editor .ql-editor td {
                    background: rgba(15, 23, 42, 0.4);
                }

                .rich-text-editor .ql-editor tr:hover td {
                    background: rgba(99, 102, 241, 0.08);
                }
            `}</style>
        </div>
    )
}

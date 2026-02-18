import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './contexts/AuthContext'
import { setAuthToken } from './lib/api'
import { useEffect } from 'react'
import NebulaBackground from './components/NebulaBackground'
import { ToastProvider } from './components/Toast'
import AuthPage from './pages/AuthPage'
import AuthCallbackPage from './pages/AuthCallbackPage'
import VerifyEmailPage from './pages/VerifyEmailPage'
import ResetPasswordPage from './pages/ResetPasswordPage'
import ForgotPasswordPage from './pages/ForgotPasswordPage'
import HomePage from './pages/HomePage'
import MeetingPage from './pages/MeetingPage'
import EmailPage from './pages/EmailPage'
import ReviewPage from './pages/ReviewPage'
import ModeSelectionPage from './pages/ModeSelectionPage'
import QuickModePage from './pages/QuickModePage'
import QuickModeReviewPage from './pages/QuickModeReviewPage'
import StructuredModePage from './pages/StructuredModePage'
import ContentEditorPage from './pages/ContentEditorPage'
import TemplateManagementPage from './pages/TemplateManagementPage'
import ContactsPage from './pages/ContactsPage'
import GroupsPage from './pages/GroupsPage'
import TechSupportPage from './pages/TechSupportPage'
import HistoryPage from './pages/HistoryPage'
import VoiceModePage from './pages/VoiceModePage'
import ProfilePage from './pages/ProfilePage'

// Protected Route wrapper
function ProtectedRoute({ children }: { children: React.ReactNode }) {
    const { user, loading } = useAuth()

    if (loading) {
        return (
            <div className="loading-screen">
                <div className="spinner spinner-large" />
            </div>
        )
    }

    if (!user) {
        return <Navigate to="/auth" replace />
    }

    return <>{children}</>
}

// Public Route wrapper (redirect to home if logged in)
function PublicRoute({ children }: { children: React.ReactNode }) {
    const { user, loading } = useAuth()

    if (loading) {
        return (
            <div className="loading-screen">
                <div className="spinner spinner-large" />
            </div>
        )
    }

    if (user) {
        return <Navigate to="/home" replace />
    }

    return <>{children}</>
}

// Root redirect logic
function RootRedirect() {
    const { user, loading } = useAuth()
    if (loading) {
        return (
            <div className="loading-screen">
                <div className="spinner spinner-large" />
            </div>
        )
    }
    return user ? <Navigate to="/home" replace /> : <Navigate to="/auth" replace />
}

function App() {
    const { session } = useAuth()

    // Set auth token whenever session changes
    useEffect(() => {
        setAuthToken(session?.accessToken || null)
    }, [session])

    return (
        <>
            <NebulaBackground />
            <ToastProvider />
            <Routes>
                <Route path="/" element={<RootRedirect />} />
                <Route
                    path="/auth"
                    element={
                        <PublicRoute>
                            <AuthPage />
                        </PublicRoute>
                    }
                />
                <Route path="/auth/callback" element={<AuthCallbackPage />} />
                <Route path="/verify-email" element={<VerifyEmailPage />} />
                <Route path="/reset-password" element={<ResetPasswordPage />} />
                <Route path="/forgot-password" element={<ForgotPasswordPage />} />
                <Route
                    path="/home"
                    element={
                        <ProtectedRoute>
                            <HomePage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/meeting"
                    element={
                        <ProtectedRoute>
                            <MeetingPage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/email"
                    element={
                        <ProtectedRoute>
                            <EmailPage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/review"
                    element={
                        <ProtectedRoute>
                            <ReviewPage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/minutes/new"
                    element={
                        <ProtectedRoute>
                            <ModeSelectionPage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/minutes/quick"
                    element={
                        <ProtectedRoute>
                            <QuickModePage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/minutes/structured"
                    element={
                        <ProtectedRoute>
                            <StructuredModePage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/minutes/voice"
                    element={
                        <ProtectedRoute>
                            <VoiceModePage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/minutes/quick/review"
                    element={
                        <ProtectedRoute>
                            <QuickModeReviewPage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/minutes/editor"
                    element={
                        <ProtectedRoute>
                            <ContentEditorPage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/templates"
                    element={
                        <ProtectedRoute>
                            <TemplateManagementPage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/contacts"
                    element={
                        <ProtectedRoute>
                            <ContactsPage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/groups"
                    element={
                        <ProtectedRoute>
                            <GroupsPage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/history"
                    element={
                        <ProtectedRoute>
                            <HistoryPage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/tech-support"
                    element={
                        <ProtectedRoute>
                            <TechSupportPage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/profile"
                    element={
                        <ProtectedRoute>
                            <ProfilePage />
                        </ProtectedRoute>
                    }
                />
                <Route path="*" element={<Navigate to="/home" replace />} />
            </Routes>
        </>
    )
}

export default App

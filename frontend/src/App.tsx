import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './contexts/AuthContext'
import { setAuthToken } from './lib/api'
import { useEffect } from 'react'
import NebulaBackground from './components/NebulaBackground'
import AuthPage from './pages/AuthPage'
import HomePage from './pages/HomePage'
import MeetingPage from './pages/MeetingPage'
import EmailPage from './pages/EmailPage'
import ReviewPage from './pages/ReviewPage'

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

function App() {
    const { session } = useAuth()

    // Set auth token whenever session changes
    useEffect(() => {
        setAuthToken(session?.access_token || null)
    }, [session])

    return (
        <>
            <NebulaBackground />
            <Routes>
                <Route path="/" element={<Navigate to="/home" replace />} />
                <Route
                    path="/auth"
                    element={
                        <PublicRoute>
                            <AuthPage />
                        </PublicRoute>
                    }
                />
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
                <Route path="*" element={<Navigate to="/home" replace />} />
            </Routes>
        </>
    )
}

export default App

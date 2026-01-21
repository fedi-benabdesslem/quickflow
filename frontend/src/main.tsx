import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App'
import { AuthProvider } from './contexts/AuthContext'
import { ReviewProvider } from './contexts/ReviewContext'

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <BrowserRouter>
            <AuthProvider>
                <ReviewProvider>
                    <App />
                </ReviewProvider>
            </AuthProvider>
        </BrowserRouter>
    </StrictMode>,
)

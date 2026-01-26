import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App'
import { AuthProvider } from './contexts/AuthContext'
import { ReviewProvider } from './contexts/ReviewContext'
import { SidebarProvider } from './contexts/SidebarContext'

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <BrowserRouter>
            <AuthProvider>
                <SidebarProvider>
                    <ReviewProvider>
                        <App />
                    </ReviewProvider>
                </SidebarProvider>
            </AuthProvider>
        </BrowserRouter>
    </StrictMode>,
)


import { createContext, useContext, useState, ReactNode } from 'react'

interface SidebarContextType {
    isOpen: boolean
    openSidebar: () => void
    closeSidebar: () => void
    toggleSidebar: () => void
    isHistoryOpen: boolean
    openHistorySidebar: () => void
    closeHistorySidebar: () => void
    toggleHistorySidebar: () => void
}

const SidebarContext = createContext<SidebarContextType | undefined>(undefined)

export function SidebarProvider({ children }: { children: ReactNode }) {
    const [isOpen, setIsOpen] = useState(false)
    const [isHistoryOpen, setIsHistoryOpen] = useState(false)

    const openSidebar = () => setIsOpen(true)
    const closeSidebar = () => setIsOpen(false)
    const toggleSidebar = () => setIsOpen(prev => !prev)

    const openHistorySidebar = () => setIsHistoryOpen(true)
    const closeHistorySidebar = () => setIsHistoryOpen(false)
    const toggleHistorySidebar = () => setIsHistoryOpen(prev => !prev)

    return (
        <SidebarContext.Provider value={{
            isOpen, openSidebar, closeSidebar, toggleSidebar,
            isHistoryOpen, openHistorySidebar, closeHistorySidebar, toggleHistorySidebar
        }}>
            {children}
        </SidebarContext.Provider>
    )
}

export function useSidebar(): SidebarContextType {
    const context = useContext(SidebarContext)
    if (context === undefined) {
        throw new Error('useSidebar must be used within a SidebarProvider')
    }
    return context
}

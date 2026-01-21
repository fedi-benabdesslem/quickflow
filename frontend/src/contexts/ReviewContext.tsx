import { createContext, useContext, useState, ReactNode } from 'react'
import type { ReviewData, ReviewContextType } from '../types'

const ReviewContext = createContext<ReviewContextType | undefined>(undefined)

export function ReviewProvider({ children }: { children: ReactNode }) {
    const [reviewData, setReviewData] = useState<ReviewData | null>(null)

    return (
        <ReviewContext.Provider value={{ reviewData, setReviewData }}>
            {children}
        </ReviewContext.Provider>
    )
}

export function useReview(): ReviewContextType {
    const context = useContext(ReviewContext)
    if (context === undefined) {
        throw new Error('useReview must be used within a ReviewProvider')
    }
    return context
}

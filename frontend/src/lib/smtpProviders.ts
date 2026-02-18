/**
 * SMTP provider instructions and configuration data.
 * Maps email domains to provider-specific setup instructions.
 */

export interface SmtpProvider {
    name: string
    domains: string[]
    instructions: string[]
    directLink?: string
    note?: string
    isBlocked?: boolean
    blockedMessage?: string
}

export const smtpProviders: SmtpProvider[] = [
    {
        name: 'Yahoo Mail',
        domains: ['yahoo.com', 'ymail.com'],
        instructions: [
            'Go to login.yahoo.com and sign in',
            'Click your profile icon → "Account Info"',
            'Click "Account Security" in the left menu',
            'Scroll down to "Generate app password" (You need 2-Step Verification enabled first)',
            'Select "Other App" and name it "QuickFlow"',
            'Click "Generate"',
            'Copy the 16-character password shown',
            'Paste it in the field below',
        ],
        directLink: 'https://login.yahoo.com/account/security',
    },
    {
        name: 'iCloud Mail',
        domains: ['icloud.com', 'me.com', 'mac.com'],
        instructions: [
            'Go to appleid.apple.com and sign in',
            'Click "Sign-In and Security"',
            'Click "App-Specific Passwords"',
            'Click the "+" button',
            'Name it "QuickFlow"',
            'Click "Create"',
            'Copy the password shown (format: xxxx-xxxx-xxxx-xxxx)',
            'Paste it in the field below',
        ],
        directLink: 'https://appleid.apple.com/account/manage',
        note: 'You need Two-Factor Authentication enabled on your Apple ID. Most Apple IDs already have this enabled.',
    },
    {
        name: 'AOL Mail',
        domains: ['aol.com'],
        instructions: [
            'Go to login.aol.com and sign in',
            'Click your profile icon → "Account Security"',
            'Scroll to "Generate app password" (Enable 2-Step Verification first if not already on)',
            'Select "Other App" and name it "QuickFlow"',
            'Click "Generate"',
            'Copy the generated password',
            'Paste it in the field below',
        ],
        directLink: 'https://login.aol.com/account/security',
    },
    {
        name: 'Zoho Mail',
        domains: ['zoho.com', 'zohomail.com'],
        instructions: [
            'Go to accounts.zoho.com and sign in',
            'Click "Security" in the left menu',
            'Scroll to "App-Specific Passwords"',
            'Click "Generate New Password"',
            'Name it "QuickFlow"',
            'Click "Generate"',
            'Copy the password shown',
            'Paste it in the field below',
        ],
        directLink: 'https://accounts.zoho.com/home#security/security',
    },
    {
        name: 'Fastmail',
        domains: ['fastmail.com'],
        instructions: [
            'Go to app.fastmail.com and sign in',
            'Go to Settings → Privacy & Security',
            'Under "Third-party apps", click "New App Password"',
            'Name it "QuickFlow"',
            'Select access: "SMTP" only',
            'Click "Generate Password"',
            'Copy the password shown',
            'Paste it in the field below',
        ],
    },
    {
        name: 'GMX Mail',
        domains: ['gmx.com', 'gmx.net'],
        instructions: [
            'Go to gmx.com and sign in',
            'Go to Settings → POP3 & IMAP',
            'Enable "Access via POP3 and IMAP"',
            'Your app password is your regular GMX password',
            'Paste it in the field below',
        ],
        note: 'GMX uses your regular password for SMTP access.',
    },
    {
        name: 'Outlook',
        domains: ['outlook.com', 'hotmail.com', 'live.com'],
        instructions: [
            'Go to account.microsoft.com and sign in',
            'Click "Security" → "Advanced security options"',
            'Under "App passwords", click "Create a new app password" (You need 2-Step Verification enabled first)',
            'Copy the password shown',
            'Paste it in the field below',
        ],
        directLink: 'https://account.microsoft.com/security',
        note: 'Tip: You can also sign out and sign in again using "Sign in with Microsoft" for a smoother experience.',
    },
    {
        name: 'Yandex Mail',
        domains: ['yandex.com', 'yandex.ru'],
        instructions: [
            'Go to passport.yandex.com and sign in',
            'Click "App Passwords"',
            'Select "Create new password"',
            'Choose type: "Mail"',
            'Name it "QuickFlow"',
            'Copy the generated password',
            'Paste it in the field below',
        ],
        directLink: 'https://passport.yandex.com/profile',
    },
    {
        name: 'Mail.com',
        domains: ['mail.com'],
        instructions: [
            'Go to mail.com and sign in',
            'Go to Settings → POP3 & IMAP',
            'Enable "Access via POP3 and IMAP"',
            'Your app password is your regular Mail.com password',
            'Paste it in the field below',
        ],
    },
    {
        name: 'ProtonMail',
        domains: ['protonmail.com', 'proton.me'],
        instructions: [],
        isBlocked: true,
        blockedMessage:
            'ProtonMail does not allow third-party apps to send emails. This is a ProtonMail security restriction, not a limitation of our app. ProtonMail encrypts all emails end-to-end and intentionally blocks external SMTP access. You can still use all other features (meeting minutes, PDF generation, etc.) — you just won\'t be able to send emails directly.',
    },
]

/**
 * Find the SMTP provider for a given email domain.
 */
export function getProviderByDomain(domain: string): SmtpProvider | undefined {
    const lowerDomain = domain.toLowerCase().trim()
    return smtpProviders.find((p) => p.domains.includes(lowerDomain))
}

/**
 * Extract domain from email address.
 */
export function extractDomain(email: string): string | null {
    if (!email || !email.includes('@')) return null
    return email.split('@')[1].toLowerCase().trim()
}

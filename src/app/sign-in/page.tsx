'use client'

import { ClerkProvider, SignIn } from '@clerk/nextjs'

export default function SignInPage() {
  return (
    <ClerkProvider publishableKey="pk_test_Y2hhbXBpb24tc29sZS05OS5jbGVyay5hY2NvdW50cy5kZXYk">
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        background: '#f5f5f5',
        fontFamily: '-apple-system, BlinkMacSystemFont, sans-serif',
      }}>
        <SignIn
          routing="path"
          path="/sign-in"
          fallbackRedirectUrl="https://my-project-five-sepia.vercel.app/api/auth/clerk-redirect"
        />
      </div>
    </ClerkProvider>
  )
}

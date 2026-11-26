import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './app/App'
import { ApiError } from './shared/api/client'
import { AuthProvider } from './shared/auth/AuthContext'
import { toast } from './shared/ui/toast'
import { ToastViewport } from './shared/ui/ToastViewport'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
      refetchOnWindowFocus: false,
    },
    mutations: {
      // One place that reports failed writes; 403 was already toasted by the HTTP layer.
      onError: (error) => {
        if (error instanceof ApiError && error.handled) return
        toast.error(error.message)
      },
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <App />
          <ToastViewport />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)

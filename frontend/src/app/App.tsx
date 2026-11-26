import { Route, Routes } from 'react-router-dom'
import { LoginPage } from '../features/auth/LoginPage'
import { DashboardPage } from '../features/dashboard/DashboardPage'
import { IssuesPage } from '../features/issues/IssuesPage'
import { ParseInvoicePage } from '../features/receipts/ParseInvoicePage'
import { ReceiptsPage } from '../features/receipts/ReceiptsPage'
import { ProductsPage } from '../features/products/ProductsPage'
import { StockSummaryPage } from '../features/reports/StockSummaryPage'
import { RequireAuth, RequireRole } from '../shared/auth/AuthContext'
import { Layout } from './Layout'
import { NotFoundPage } from './NotFoundPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="products" element={<ProductsPage />} />
        <Route path="receipts" element={<ReceiptsPage />} />
        <Route path="receipts/parse" element={<ParseInvoicePage />} />
        <Route path="issues" element={<IssuesPage />} />
        <Route
          path="reports"
          element={
            <RequireRole roles={['ADMIN']}>
              <StockSummaryPage />
            </RequireRole>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

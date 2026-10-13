import { Navigate } from 'react-router-dom'
import { useAuth } from '../AuthContext'

export default function RouteProtegee({ roles, children }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/connexion" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />
  return children
}
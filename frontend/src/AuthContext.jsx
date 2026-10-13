import { createContext, useContext, useState } from 'react'

const AuthContext = createContext(null)

function lireUtilisateur() {
  try {
    const brut = localStorage.getItem('user')
    return brut ? JSON.parse(brut) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(lireUtilisateur)

  function connecter(reponse) {
    const { token, ...infos } = reponse
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(infos))
    setUser(infos)
  }

  function deconnecter() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, connecter, deconnecter }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
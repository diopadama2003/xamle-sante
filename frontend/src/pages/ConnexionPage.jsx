import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../api'
import { useAuth } from '../AuthContext'

export default function ConnexionPage() {
  const { connecter } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [motDePasse, setMotDePasse] = useState('')
  const [erreur, setErreur] = useState('')
  const [envoi, setEnvoi] = useState(false)

  async function soumettre(e) {
    e.preventDefault()
    setErreur('')
    setEnvoi(true)
    try {
      const { data } = await api.post('/auth/login', { email, motDePasse })
      connecter(data)
      navigate(data.role === 'PROFESSIONNEL' ? '/pro' : '/')
    } catch (err) {
      if (err.response?.status === 401) {
        setErreur('Email ou mot de passe incorrect')
      } else if (err.response?.status === 400) {
        setErreur("Vérifie l'email et le mot de passe")
      } else {
        setErreur('Impossible de joindre le serveur')
      }
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <div className="page">
      <form className="formulaire" onSubmit={soumettre}>
        <h2>Connexion</h2>
        <label>
          Email
          <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </label>
        <label>
          Mot de passe
          <input
            type="password"
            required
            value={motDePasse}
            onChange={(e) => setMotDePasse(e.target.value)}
          />
        </label>
        {erreur && <div className="alerte erreur">{erreur}</div>}
        <button className="bouton" type="submit" disabled={envoi}>
          {envoi ? 'Connexion...' : 'Se connecter'}
        </button>
        <p>
          Pas de compte ? <Link to="/inscription">Créer un compte</Link>
        </p>
      </form>
    </div>
  )
}
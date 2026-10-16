import { Routes, Route } from 'react-router-dom'
import Navbar from './components/Navbar'
import RouteProtegee from './components/RouteProtegee'
import CartePage from './pages/CartePage'
import ConnexionPage from './pages/ConnexionPage'
import InscriptionPage from './pages/InscriptionPage'
import EspacePro from './pages/EspacePro'
import ModerationPage from './pages/ModerationPage'
import SosPage from './pages/SosPage'
import ChaleurPage from './pages/ChaleurPage'
import SignalerPage from './pages/SignalerPage'
import './App.css'

export default function App() {
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/" element={<CartePage />} />
        <Route path="/sos" element={<SosPage />} />
        <Route path="/chaleur" element={<ChaleurPage />} />
        <Route path="/signaler" element={<SignalerPage />} />
        <Route path="/connexion" element={<ConnexionPage />} />
        <Route path="/inscription" element={<InscriptionPage />} />
        <Route
          path="/pro"
          element={
            <RouteProtegee roles={['PROFESSIONNEL']}>
              <EspacePro />
            </RouteProtegee>
          }
        />
        <Route
          path="/moderation"
          element={
            <RouteProtegee roles={['MODERATEUR', 'ADMIN_SANTE']}>
              <ModerationPage />
            </RouteProtegee>
          }
        />
      </Routes>
    </>
  )
}
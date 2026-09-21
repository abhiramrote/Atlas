import { BrowserRouter, Route, Routes } from "react-router-dom";

import DashboardPage from "./pages/DashboardPage";
import CompanyPage from "./pages/CompanyPage";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/company/:companyId" element={<CompanyPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;

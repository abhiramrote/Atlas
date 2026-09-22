import { BrowserRouter, Route, Routes } from "react-router-dom";

import DashboardPage from "./pages/DashboardPage";
import CompanyPage from "./pages/CompanyPage";
import ThesisJournalPage from "./pages/ThesisJournalPage";
import ThesisDetailPage from "./pages/ThesisDetailPage";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<DashboardPage />} />

        <Route
          path="/company/:companyId"
          element={<CompanyPage />}
        />

        <Route path="/theses" element={<ThesisJournalPage />} />

        <Route
          path="/theses/:thesisId"
          element={<ThesisDetailPage />}
        />
      </Routes>
    </BrowserRouter>
  );
}

export default App;

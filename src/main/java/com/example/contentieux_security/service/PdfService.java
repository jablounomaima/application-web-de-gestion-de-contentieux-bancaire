package com.example.contentieux_security.service;

import com.example.contentieux_security.dto.DossierDetailDTO;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import com.itextpdf.text.pdf.draw.LineSeparator;
@Service
public class PdfService {

    // ── Couleurs ──────────────────────────────────────────
    private static final BaseColor BLEU_TITRE  = new BaseColor(44, 62, 80);
    private static final BaseColor BLEU_HEADER = new BaseColor(52, 73, 94);
    private static final BaseColor VERT_VALIDE = new BaseColor(39, 174, 96);
    private static final BaseColor GRIS_LIGNE  = new BaseColor(245, 245, 245);

    // ── Polices ───────────────────────────────────────────
    private static final Font TITRE      = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,   BLEU_TITRE);
    private static final Font SOUS_TITRE = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.GRAY);
    private static final Font SECTION    = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,   BLEU_HEADER);
    private static final Font LABEL      = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BaseColor.DARK_GRAY);
    private static final Font VALEUR     = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
    private static final Font VALIDE_F   = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   VERT_VALIDE);
    private static final Font TH_F       = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BaseColor.WHITE);
    private static final Font TD_F       = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, BaseColor.BLACK);

    public byte[] genererDossierPdf(DossierDetailDTO d) throws DocumentException {

        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(doc, out);

        // ── En-tête / Pied de page ─────────────────────
        writer.setPageEvent(new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter w, Document document) {
                PdfContentByte cb = w.getDirectContent();

                // Bande bleue en haut
                cb.setColorFill(BLEU_HEADER);
                cb.rectangle(0, PageSize.A4.getHeight() - 30,
                        PageSize.A4.getWidth(), 30);
                cb.fill();

                // Texte en-tête
                try {
                    cb.beginText();
                    cb.setFontAndSize(BaseFont.createFont(), 9);
                    cb.setColorFill(BaseColor.WHITE);
                    cb.moveText(40, PageSize.A4.getHeight() - 18);
                    cb.showText("BANQUE — Dossier Contentieux Confidentiel");
                    cb.endText();
                } catch (Exception ignored) {}

                // Pied de page
                cb.setColorFill(BLEU_HEADER);
                cb.rectangle(0, 0, PageSize.A4.getWidth(), 20);
                cb.fill();

                try {
                    cb.beginText();
                    cb.setFontAndSize(BaseFont.createFont(), 8);
                    cb.setColorFill(BaseColor.WHITE);
                    cb.moveText(40, 6);
                    cb.showText("Page " + w.getPageNumber()
                            + "  |  Généré le "
                            + java.time.LocalDate.now()
                              .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    cb.endText();
                } catch (Exception ignored) {}
            }
        });

        doc.open();

        // ════════════════════════════════════════════════
        //  TITRE PRINCIPAL
        // ════════════════════════════════════════════════
        Paragraph titre = new Paragraph("DOSSIER CONTENTIEUX", TITRE);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingBefore(20);
        doc.add(titre);

        Paragraph ref = new Paragraph(
                "Réf. : " + val(d.getNumeroDossier()), SOUS_TITRE);
        ref.setAlignment(Element.ALIGN_CENTER);
        ref.setSpacingAfter(6);
        doc.add(ref);

        // Ligne de séparation verte
        LineSeparator sep = new LineSeparator(2, 100, VERT_VALIDE,
                Element.ALIGN_CENTER, -2);
        doc.add(new Chunk(sep));
        doc.add(Chunk.NEWLINE);

        // ── Badge VALIDÉ ──────────────────────────────
        Paragraph badge = new Paragraph("✅  DOSSIER ENTIÈREMENT VALIDÉ", VALIDE_F);
        badge.setAlignment(Element.ALIGN_CENTER);
        badge.setSpacingBefore(4);
        badge.setSpacingAfter(16);
        doc.add(badge);

        // ════════════════════════════════════════════════
        //  SECTION 1 — INFOS DOSSIER
        // ════════════════════════════════════════════════
        doc.add(sectionTitre("1. Informations du dossier"));

        PdfPTable infos = tableDeuxCol();
        ajouterLigne(infos, "Numéro dossier",  val(d.getNumeroDossier()),  false);
        ajouterLigne(infos, "Libellé",          val(d.getLibelle()),         true);
        ajouterLigne(infos, "Statut",           val(d.getStatut()),          false);
        ajouterLigne(infos, "Date de création",
                d.getDateCreation() != null
                ? d.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "–", true);
        ajouterLigne(infos, "Créé par",         val(d.getCreePar()),         false);
        ajouterLigne(infos, "Notes",            val(d.getNotes()),           true);
        doc.add(infos);
        doc.add(espaceur());

        // ════════════════════════════════════════════════
        //  SECTION 2 — CLIENT
        // ════════════════════════════════════════════════
        doc.add(sectionTitre("2. Informations client"));

        PdfPTable client = tableDeuxCol();
        ajouterLigne(client, "Nom",       val(d.getClientNom()),       false);
        ajouterLigne(client, "Prénom",    val(d.getClientPrenom()),    true);
        ajouterLigne(client, "CIN",       val(d.getClientCin()),       false);
        ajouterLigne(client, "Email",     val(d.getClientEmail()),     true);
        ajouterLigne(client, "Téléphone", val(d.getClientTelephone()), false);
        ajouterLigne(client, "Adresse",   val(d.getClientAdresse()),   true);
        doc.add(client);
        doc.add(espaceur());

        // ════════════════════════════════════════════════
        //  SECTION 3 — ENGAGEMENT FINANCIER
        // ════════════════════════════════════════════════
        doc.add(sectionTitre("3. Engagement financier"));

        PdfPTable finance = tableDeuxCol();
        ajouterLigne(finance, "Montant total engagement",
                d.getMontantTotalEngagement() != null
                ? String.format("%,.2f TND", d.getMontantTotalEngagement())
                : "–", false);
        doc.add(finance);
        doc.add(espaceur());

        // ════════════════════════════════════════════════
        //  SECTION 4 — RISQUES & GARANTIES
        // ════════════════════════════════════════════════
        doc.add(sectionTitre("4. Risques / Crédits"));

        if (d.getRisques() != null && !d.getRisques().isEmpty()) {
            for (DossierDetailDTO.RisqueDTO r : d.getRisques()) {

                // Sous-titre risque
                Paragraph rTitre = new Paragraph(
                        (r.isSelectionne() ? "★ " : "• ")
                        + val(r.getType())
                        + (r.isSelectionne() ? "  [SÉLECTIONNÉ]" : ""),
                        new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,
                                r.isSelectionne()
                                ? new BaseColor(52,152,219)
                                : BaseColor.DARK_GRAY));
                rTitre.setSpacingBefore(6);
                doc.add(rTitre);

                PdfPTable risque = tableDeuxCol();
                ajouterLigne(risque, "Montant initial",
                        r.getMontantInitial() != null
                        ? String.format("%,.2f TND", r.getMontantInitial()) : "–", false);
                ajouterLigne(risque, "Montant impayé",
                        r.getMontantImpaye() != null
                        ? String.format("%,.2f TND", r.getMontantImpaye()) : "–", true);
                ajouterLigne(risque, "Échéance",    val(r.getDateEcheance()),  false);
                ajouterLigne(risque, "Description", val(r.getDescription()),   true);
                doc.add(risque);

                // Garanties
                if (r.getGaranties() != null && !r.getGaranties().isEmpty()) {
                    Paragraph gTitre = new Paragraph("  Garanties :",
                            new Font(Font.FontFamily.HELVETICA, 9,
                                    Font.ITALIC, BaseColor.GRAY));
                    doc.add(gTitre);

                    PdfPTable gTable = new PdfPTable(new float[]{30, 25, 25, 20});
                    gTable.setWidthPercentage(95);
                    gTable.setSpacingBefore(2);
                    ajouterHeaderGarantie(gTable);

                    for (DossierDetailDTO.GarantieDTO g : r.getGaranties()) {
                        gTable.addCell(tdCell(val(g.getTypeGarantie()), false));
                        gTable.addCell(tdCell(val(g.getDescription()),  false));
                        gTable.addCell(tdCell(
                                g.getValeurEstimee() != null
                                ? String.format("%,.2f TND", g.getValeurEstimee()) : "–",
                                false));
                        gTable.addCell(tdCell(val(g.getDocumentRef()),  false));
                    }
                    doc.add(gTable);
                }
            }
        } else {
            doc.add(new Paragraph("Aucun risque enregistré.", VALEUR));
        }
        doc.add(espaceur());

        // ════════════════════════════════════════════════
        //  SECTION 5 — VALIDATIONS
        // ════════════════════════════════════════════════
        doc.add(sectionTitre("5. Décisions de validation"));

        PdfPTable valid = tableDeuxCol();

        // Financière
        ajouterLigne(valid, "Validation financière",
                d.getValidationFinanciere() != null && d.getValidationFinanciere()
                ? "✅ Validé" : "❌ Rejeté", false);
        ajouterLigne(valid, "Validateur financier",
                val(d.getValidateurFinancierUsername()), true);
        ajouterLigne(valid, "Commentaire financier",
                val(d.getCommentaireFinancier()), false);

        // Séparateur
        PdfPCell sepCell = new PdfPCell(new Phrase(" "));
        sepCell.setColspan(2);
        sepCell.setBorder(Rectangle.NO_BORDER);
        sepCell.setFixedHeight(6);
        valid.addCell(sepCell);

        // Juridique
        ajouterLigne(valid, "Validation juridique",
                d.getValidationJuridique() != null && d.getValidationJuridique()
                ? "✅ Validé" : "❌ Rejeté", true);
        ajouterLigne(valid, "Validateur juridique",
                val(d.getValidateurJuridiqueUsername()), false);
        ajouterLigne(valid, "Commentaire juridique",
                val(d.getCommentaireJuridique()), true);

        doc.add(valid);
        doc.add(espaceur());

        // ════════════════════════════════════════════════
        //  SECTION 6 — HISTORIQUE
        // ════════════════════════════════════════════════
        if (d.getHistorique() != null && !d.getHistorique().isEmpty()) {
            doc.add(sectionTitre("6. Historique des actions"));

            PdfPTable hist = new PdfPTable(new float[]{25, 20, 35, 20});
            hist.setWidthPercentage(100);
            hist.setSpacingBefore(6);

            // Header
            String[] headers = {"Date", "Action", "Description", "Utilisateur"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, TH_F));
                cell.setBackgroundColor(BLEU_HEADER);
                cell.setPadding(6);
                cell.setBorder(Rectangle.NO_BORDER);
                hist.addCell(cell);
            }

            boolean alt = false;
            for (DossierDetailDTO.HistoriqueEntryDTO h : d.getHistorique()) {
                BaseColor bg = alt ? GRIS_LIGNE : BaseColor.WHITE;
                hist.addCell(tdCellBg(val(h.getDateAction()),   bg));
                hist.addCell(tdCellBg(val(h.getTypeAction()),   bg));
                hist.addCell(tdCellBg(val(h.getDescription()),  bg));
                hist.addCell(tdCellBg(val(h.getUtilisateur()),  bg));
                alt = !alt;
            }
            doc.add(hist);
        }

        // ── Signature ─────────────────────────────────
        doc.add(espaceur());
        doc.add(espaceur());
        LineSeparator sepFin = new LineSeparator(1, 100,
                BaseColor.LIGHT_GRAY, Element.ALIGN_CENTER, -2);
        doc.add(new Chunk(sepFin));

        Paragraph sign = new Paragraph(
                "Document généré automatiquement — Système de gestion contentieux",
                new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY));
        sign.setAlignment(Element.ALIGN_CENTER);
        sign.setSpacingBefore(6);
        doc.add(sign);

        doc.close();
        return out.toByteArray();
    }

    // ════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════

    private Paragraph sectionTitre(String texte) {
        Paragraph p = new Paragraph(texte, SECTION);
        p.setSpacingBefore(14);
        p.setSpacingAfter(6);

        // Filet sous le titre de section
        p.add(new Chunk(new LineSeparator(1, 100,
                new BaseColor(189, 195, 199), Element.ALIGN_LEFT, -4)));
        return p;
    }

    private PdfPTable tableDeuxCol() throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{35, 65});
        t.setWidthPercentage(100);
        t.setSpacingBefore(4);
        return t;
    }

    private void ajouterLigne(PdfPTable t, String label,
                               String valeur, boolean alt) {
        BaseColor bg = alt ? GRIS_LIGNE : BaseColor.WHITE;

        PdfPCell cLabel = new PdfPCell(new Phrase(label, LABEL));
        cLabel.setBackgroundColor(bg);
        cLabel.setPadding(6);
        cLabel.setBorder(Rectangle.NO_BORDER);
        t.addCell(cLabel);

        PdfPCell cVal = new PdfPCell(new Phrase(valeur, VALEUR));
        cVal.setBackgroundColor(bg);
        cVal.setPadding(6);
        cVal.setBorder(Rectangle.NO_BORDER);
        t.addCell(cVal);
    }

    private void ajouterHeaderGarantie(PdfPTable t) {
        String[] cols = {"Type garantie", "Description", "Valeur estimée", "Réf. document"};
        for (String c : cols) {
            PdfPCell cell = new PdfPCell(new Phrase(c, TH_F));
            cell.setBackgroundColor(new BaseColor(127, 140, 141));
            cell.setPadding(5);
            cell.setBorder(Rectangle.NO_BORDER);
            t.addCell(cell);
        }
    }

    private PdfPCell tdCell(String text, boolean alt) {
        PdfPCell c = new PdfPCell(new Phrase(text, TD_F));
        c.setBackgroundColor(alt ? GRIS_LIGNE : BaseColor.WHITE);
        c.setPadding(5);
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private PdfPCell tdCellBg(String text, BaseColor bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, TD_F));
        c.setBackgroundColor(bg);
        c.setPadding(5);
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private Paragraph espaceur() {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(4);
        return p;
    }

    private String val(String s) {
        return s != null && !s.isBlank() ? s : "–";
    }
}
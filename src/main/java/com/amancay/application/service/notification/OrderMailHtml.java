package com.amancay.application.service.notification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.amancay.application.port.out.MailContent;
import com.amancay.domain.model.OrderStatus;

// Plantilla HTML de los mails de pedidos, con la identidad del front (colores de
// amancay2.0-front/src/index.css, tipografia Nunito y el logo). Usa tablas y estilos inline
// porque es lo unico que respetan todos los clientes de mail (Gmail, Outlook, etc.).
final class OrderMailHtml {

    private static final String PRIMARY = "#2e5e4e";
    private static final String CREAM = "#fffcf4";
    private static final String INFO = "#f7e45d";
    private static final String MIST = "#f1f4f1";
    private static final String DARK = "#333333";
    private static final String MUTED = "#707070";
    private static final String BORDER = "#ebebeb";
    private static final String FONT = "'Nunito', 'Segoe UI', Helvetica, Arial, sans-serif";

    private static final Map<OrderStatus, String> STATUS_LABELS = new EnumMap<>(Map.of(
            OrderStatus.CREADO, "Creado",
            OrderStatus.EN_PREPARACION, "En preparación",
            OrderStatus.LISTO_PARA_DESPACHAR, "Listo para despachar",
            OrderStatus.DESPACHADO, "Despachado",
            OrderStatus.ENTREGADO, "Entregado",
            OrderStatus.ESPERANDO_RETIRO, "Esperando retiro",
            OrderStatus.RETIRADO, "Retirado",
            OrderStatus.DEVUELTO, "Devuelto"));

    record Line(String name, String imageUrl, int quantity, BigDecimal subtotal) {
    }

    record View(String orderNumber, OrderStatus status, String headline, String intro, List<Line> lines,
            BigDecimal subtotal, BigDecimal shippingCost, BigDecimal total, String closing) {
    }

    private OrderMailHtml() {
    }

    static String render(View view) {
        StringBuilder html = new StringBuilder(4096);
        html.append("<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<link href=\"https://fonts.googleapis.com/css2?family=Nunito:wght@400;600;700;800&display=swap\" rel=\"stylesheet\">")
                .append("<title>").append(escape(view.headline())).append("</title></head>")
                .append("<body style=\"margin:0;padding:0;background:").append(MIST).append(";\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:")
                .append(MIST).append(";font-family:").append(FONT).append(";color:").append(DARK).append(";\">")
                .append("<tr><td align=\"center\" style=\"padding:24px 12px;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;")
                .append("background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid ").append(BORDER).append(";\">");

        // Marca
        html.append("<tr><td style=\"padding:20px 28px;border-bottom:1px solid ").append(BORDER).append(";\">")
                .append("<img src=\"cid:").append(MailContent.LOGO_CID).append("\" alt=\"\" height=\"36\" ")
                .append("style=\"height:36px;width:auto;vertical-align:middle;border:0;\">")
                .append("<span style=\"font-size:20px;font-weight:800;letter-spacing:0.5px;color:").append(PRIMARY)
                .append(";vertical-align:middle;margin-left:8px;\">AMANCAY</span></td></tr>");

        // Encabezado: numero de pedido, titulo y estado
        html.append("<tr><td style=\"background:").append(PRIMARY).append(";padding:28px;\">")
                .append("<p style=\"margin:0 0 6px;font-size:13px;font-weight:600;color:#a9d6c6;\">Pedido #")
                .append(escape(view.orderNumber())).append("</p>")
                .append("<h1 style=\"margin:0 0 14px;font-size:26px;line-height:1.25;font-weight:800;color:").append(CREAM)
                .append(";\">").append(escape(view.headline())).append("</h1>")
                .append("<span style=\"display:inline-block;background:").append(INFO).append(";color:").append(DARK)
                .append(";border-radius:999px;padding:4px 12px;font-size:12px;font-weight:700;\">")
                .append(escape(STATUS_LABELS.getOrDefault(view.status(), view.status().name()))).append("</span>")
                .append("</td></tr>");

        // Mensaje
        html.append("<tr><td style=\"padding:28px 28px 8px;font-size:15px;line-height:1.6;\">")
                .append(paragraphs(view.intro())).append("</td></tr>");

        // Productos
        html.append("<tr><td style=\"padding:8px 28px;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">");
        for (Line line : view.lines()) {
            html.append("<tr><td width=\"56\" style=\"padding:10px 0;border-top:1px solid ").append(BORDER).append(";\">");
            if (line.imageUrl() != null && !line.imageUrl().isBlank()) {
                html.append("<img src=\"").append(escape(line.imageUrl())).append("\" alt=\"\" width=\"44\" height=\"44\" ")
                        .append("style=\"display:block;width:44px;height:44px;border-radius:8px;object-fit:cover;background:")
                        .append(MIST).append(";border:0;\">");
            } else {
                html.append("<div style=\"width:44px;height:44px;border-radius:8px;background:").append(MIST).append(";\"></div>");
            }
            html.append("</td><td style=\"padding:10px 8px;border-top:1px solid ").append(BORDER).append(";font-size:14px;\">")
                    .append("<span style=\"font-weight:700;\">").append(escape(line.name())).append("</span><br>")
                    .append("<span style=\"color:").append(MUTED).append(";font-size:13px;\">Cantidad: ")
                    .append(line.quantity()).append("</span></td>")
                    .append("<td align=\"right\" style=\"padding:10px 0;border-top:1px solid ").append(BORDER)
                    .append(";font-size:14px;font-weight:700;white-space:nowrap;\">").append(money(line.subtotal()))
                    .append("</td></tr>");
        }
        html.append("</table></td></tr>");

        // Totales
        html.append("<tr><td style=\"padding:4px 28px 24px;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:")
                .append(MIST).append(";border-radius:12px;font-size:14px;\">")
                .append(totalRow("Subtotal", view.subtotal(), false))
                .append(totalRow("Envío", view.shippingCost(), false))
                .append(totalRow("Total", view.total(), true))
                .append("</table></td></tr>");

        // Cierre
        if (view.closing() != null && !view.closing().isBlank()) {
            html.append("<tr><td style=\"padding:0 28px 28px;font-size:14px;line-height:1.6;color:").append(MUTED)
                    .append(";\">").append(paragraphs(view.closing())).append("</td></tr>");
        }

        html.append("<tr><td style=\"background:").append(MIST).append(";padding:18px 28px;font-size:12px;color:")
                .append(MUTED).append(";text-align:center;\">Recibiste este mail porque compraste en <strong style=\"color:")
                .append(PRIMARY).append(";\">Amancay</strong>.</td></tr>")
                .append("</table></td></tr></table></body></html>");
        return html.toString();
    }

    private static String totalRow(String label, BigDecimal amount, boolean grand) {
        String size = grand ? "17px" : "14px";
        String weight = grand ? "800" : "600";
        String color = grand ? PRIMARY : DARK;
        String border = grand ? "border-top:1px solid " + BORDER + ";" : "";
        return "<tr><td style=\"padding:" + (grand ? "12px" : "8px") + " 16px;" + border + "font-size:" + size
                + ";font-weight:" + weight + ";color:" + color + ";\">" + label + "</td>"
                + "<td align=\"right\" style=\"padding:" + (grand ? "12px" : "8px") + " 16px;" + border + "font-size:"
                + size + ";font-weight:" + weight + ";color:" + color + ";white-space:nowrap;\">" + money(amount)
                + "</td></tr>";
    }

    // Cada linea en blanco separa un parrafo; un salto simple queda como <br>.
    private static String paragraphs(String text) {
        StringBuilder html = new StringBuilder();
        for (String paragraph : text.strip().split("\\n\\s*\\n")) {
            html.append("<p style=\"margin:0 0 12px;\">")
                    .append(escape(paragraph.strip()).replace("\n", "<br>"))
                    .append("</p>");
        }
        return html.toString();
    }

    static String money(BigDecimal amount) {
        return "$" + (amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}

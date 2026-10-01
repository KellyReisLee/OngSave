package br.com.ongsave.util;

import java.util.Map;

/** Validação de ficheiros enviados pelos utilizadores (pela assinatura, não pela extensão). */
public final class Imagens {
    private Imagens() {}

    private static final Map<String, byte[]> ASSINATURAS = Map.of(
            "image/jpeg", new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF },
            "image/png", new byte[] { (byte) 0x89, 'P', 'N', 'G' },
            "image/webp", new byte[] { 'R', 'I', 'F', 'F' },
            "application/pdf", new byte[] { '%', 'P', 'D', 'F' });

    /** Devolve o content-type real do ficheiro ou null se não for JPEG, PNG, WEBP, HEIC ou PDF. */
    public static String detectar(byte[] b) {
        if (b == null || b.length < 12) return null;
        for (Map.Entry<String, byte[]> e : ASSINATURAS.entrySet()) {
            byte[] a = e.getValue();
            boolean ok = true;
            for (int i = 0; i < a.length; i++) if (b[i] != a[i]) { ok = false; break; }
            if (ok) {
                if (e.getKey().equals("image/webp") && !(b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P')) continue;
                return e.getKey();
            }
        }
        // HEIC/HEIF (fotos de iPhone): "ftyp" no byte 4 e marca heic/heix/mif1
        if (b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') {
            String marca = new String(b, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if (marca.startsWith("hei") || marca.startsWith("mif") || marca.startsWith("hev")) return "image/heic";
        }
        return null;
    }

    public static boolean ehImagem(String tipo) {
        return tipo != null && tipo.startsWith("image/");
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author mateu
 */
public class Mtm extends JFrame {
 
    //parâmetros do filtro
    private final int tam; //lado da janela (mediana e média usam a mesma)
    private final double c; //limiar de corte em torno da mediana
 
    //imagens e rasters
    private int w, h;
    private BufferedImage imgOriginal, imgPro;
    private Raster origRaster;
    private WritableRaster procRaster;
 
    public static void main(String[] args) {
        String nomeImg = "seminario.jpeg";
        int tam = 5;
        double c = 30;
 
        JFrame.setDefaultLookAndFeelDecorated(true);
 
        Mtm jf = new Mtm(new File(nomeImg), tam, c);
 
        jf.aplicarMtm();
 
        jf.setTitle(String.format(
                "MTM (janela=%dx%d, C=%.1f) - %s | PSNR vs original = %.2f dB",
                tam, tam, c, nomeImg, jf.calcularPsnr()));
 
        // GridLayout 2x1: original em cima, processada embaixo
        jf.getContentPane().setLayout(new GridLayout(2, 1, 5, 5));
        jf.getContentPane().add(painel("Original", jf.imgOriginal));
        jf.getContentPane().add(painel("Processada (MTM)", jf.imgPro));
 
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setSize(600, 900);
        jf.setLocationRelativeTo(null);
        jf.setVisible(true);
    }

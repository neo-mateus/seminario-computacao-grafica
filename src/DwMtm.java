
import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author mateu
 */
public class DwMtm extends Frame{
    
    //parâmetros do filtro
    private final int n;
    private final int q;
    private final double c;
    
    //imagens e rasters
    private int w, h;
    private BufferedImage imgOriginal, imgPro;
    private Raster origRaster;
    private WritableRaster procRaster;
    
}

import java.time.chrono.*;
import java.time.temporal.*;
import java.nio.file.*;
public class GenerateUmmAlQura {
 public static void main(String[] args) throws Exception {
  StringBuilder s=new StringBuilder("package com.salati.shared\n\n// Generated from OpenJDK Hijrah-umalqura, AH 1300–1600. See tools/migration/GenerateUmmAlQura.java.\ninternal val ummAlQuraMonthStarts = intArrayOf(\n");
  for(int y=1300;y<=1600;y++) { for(int m=1;m<=12;m++) s.append(HijrahChronology.INSTANCE.date(y,m,1).toEpochDay()).append(","); s.append("\n"); }
  var end=HijrahChronology.INSTANCE.date(1600,12,1); s.append(end.toEpochDay()+end.lengthOfMonth()).append("\n)\n");
  Files.writeString(Path.of(args[0]),s);
 }
}

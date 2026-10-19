package com.aptisprep.dto.form;
import java.util.*;
import jakarta.validation.constraints.*;
public class FlashcardSetForm {
 @NotBlank @Size(max=255) private String title;
 @Size(max=2000) private String description;
 @NotBlank private String cefrLevel="A1";
 @NotBlank private String status="DRAFT";
 private List<Row> rows=new ArrayList<>();
 public static class Row {
  private Integer id; @Size(max=200) private String term; @Size(max=1000) private String definition;
  public Integer getId(){return id;} public void setId(Integer v){id=v;}
  public String getTerm(){return term;} public void setTerm(String v){term=v;}
  public String getDefinition(){return definition;} public void setDefinition(String v){definition=v;}
 }
 public String getTitle(){return title;} public void setTitle(String v){title=v;}
 public String getDescription(){return description;} public void setDescription(String v){description=v;}
 public String getCefrLevel(){return cefrLevel;} public void setCefrLevel(String v){cefrLevel=v;}
 public String getStatus(){return status;} public void setStatus(String v){status=v;}
 public List<Row> getRows(){return rows;} public void setRows(List<Row> v){rows=v;}
}

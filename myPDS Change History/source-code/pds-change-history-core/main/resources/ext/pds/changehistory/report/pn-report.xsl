<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format">
    
    <xsl:attribute-set name="report-title">
        <xsl:attribute name="font-size">10pt</xsl:attribute>
        <xsl:attribute name="font-family">Arial</xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="text-align">center</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="table-inline-title">
        <xsl:attribute name="font-size">7pt</xsl:attribute>
        <xsl:attribute name="font-family">Arial</xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="background-color">#D3D3D3</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="header-table">
        <xsl:attribute name="border">0pt solid black</xsl:attribute>
        <xsl:attribute name="border-bottom">1pt solid black</xsl:attribute>
        <xsl:attribute name="padding-bottom">3mm</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="table-cell">
        <xsl:attribute name="padding">1mm</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="centered">
        <xsl:attribute name="text-align">center</xsl:attribute>
    </xsl:attribute-set>
    
    <xsl:attribute-set name="green">
        <xsl:attribute name="background-color">#B5CF89</xsl:attribute>
    </xsl:attribute-set>
    
    <xsl:attribute-set name="blue">
        <xsl:attribute name="background-color">#D2EAF0</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="yellow">
        <xsl:attribute name="background-color">#FFFFC1</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="table-cell-grid">
        <xsl:attribute name="border">1pt solid black</xsl:attribute>
        <xsl:attribute name="padding">1mm</xsl:attribute>
    </xsl:attribute-set>
    
    <xsl:attribute-set name="table-cell-no-border">
        <xsl:attribute name="border">0pt</xsl:attribute>
    </xsl:attribute-set>
    
    <xsl:attribute-set name="table-cell-missing-bottom">
        <xsl:attribute name="border-top">1pt solid black</xsl:attribute>
        <xsl:attribute name="border-left">1pt solid black</xsl:attribute>
        <xsl:attribute name="border-right">1pt solid black</xsl:attribute>
        <xsl:attribute name="padding">1mm</xsl:attribute>
    </xsl:attribute-set>
    
    <xsl:attribute-set name="table-cell-missing-top">
        <xsl:attribute name="border-bottom">1pt solid black</xsl:attribute>
        <xsl:attribute name="border-left">1pt solid black</xsl:attribute>
        <xsl:attribute name="border-right">1pt solid black</xsl:attribute>
        <xsl:attribute name="padding">1mm</xsl:attribute>
    </xsl:attribute-set>
    
    <xsl:attribute-set name="table-row-last">
        <xsl:attribute name="border-bottom">1pt solid black</xsl:attribute>
    </xsl:attribute-set>
    
    <xsl:attribute-set name="table-row-bom">
        <xsl:attribute name="border-top">1pt solid black</xsl:attribute>
    </xsl:attribute-set>

    <xsl:template match="report">
        <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">

            <fo:layout-master-set>
                <fo:simple-page-master master-name="A4">
                    <fo:region-body margin="8mm" margin-top="40mm" />
                    <fo:region-before extent="40mm" />
                </fo:simple-page-master>
            </fo:layout-master-set>

            <fo:page-sequence master-reference="A4" id="seq1">
                <fo:static-content flow-name="xsl-region-before">
                    <fo:block margin="4.2mm" font-family="Calibri" font-size="9pt">
                        <fo:table table-layout="fixed" >
                            <fo:table-column column-width="25%" />
                            <fo:table-column column-width="50%" />
                            <fo:table-column column-width="25%" />
                            
                            <fo:table-body>
                                <fo:table-row>
                                    <fo:table-cell xsl:use-attribute-sets="header-table">
                                        <fo:block>Document: <xsl:value-of select="//header/docName" /></fo:block>
                                        <fo:block>Document rev: <xsl:value-of select="//header/docRev" /></fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell xsl:use-attribute-sets="header-table">
                                        <fo:block xsl:use-attribute-sets="report-title">ÄNDERUNGSMITTEILUNG zum ARTIKEL (INDEX)</fo:block>
                                        <fo:block xsl:use-attribute-sets="report-title">Change Notice for article-release</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell xsl:use-attribute-sets="header-table">
                                        <fo:block-container position="absolute" height="25cm" 
                                                            background-image="url('logo-pds.png')"
                                                            background-color="transparent" background-repeat="no-repeat">
                                            <fo:block/>
                                        </fo:block-container>
                                    </fo:table-cell>
                                </fo:table-row>
                            </fo:table-body>
                        </fo:table>
                    </fo:block>
                </fo:static-content>
                <fo:flow flow-name="xsl-region-body" font-family="Calibri" font-size="9pt">
                    <fo:block>
                        <xsl:apply-templates select="parts"/>
                    </fo:block>
                </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>
	
    <xsl:template match="parts">
        <xsl:param name = "pnNumber" />
        <xsl:apply-templates select="part">
            <xsl:with-param name="pnNumber" >
                <xsl:value-of select="//pn/number" />
            </xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>    
        
    <xsl:template match="part">
        <xsl:param name = "pnNumber" />
        <fo:block page-break-before="always">
            <fo:table table-layout="fixed" >
                <fo:table-column column-width="22%" />
                <fo:table-column column-width="22%" />
                <fo:table-column column-width="16%" />
                <fo:table-column column-width="20%" />
                <fo:table-column column-width="20%" />
            			
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell xsl:use-attribute-sets="table-cell-grid table-inline-title">
                            <fo:block>Änderungssnummer / Count of release</fo:block>
                        </fo:table-cell>
                        <fo:table-cell xsl:use-attribute-sets="table-cell-grid table-inline-title">
                            <fo:block>Artikel-Nr. / part-ID</fo:block>
                        </fo:table-cell>
                        <fo:table-cell xsl:use-attribute-sets="table-cell-grid table-inline-title">
                            <fo:block>Index / release</fo:block>
                        </fo:table-cell>
                        <fo:table-cell xsl:use-attribute-sets="table-cell-grid table-inline-title">
                            <fo:block>Name / name</fo:block>
                        </fo:table-cell>
                        <fo:table-cell xsl:use-attribute-sets="table-cell-grid table-inline-title">
                            <fo:block>Datum / date</fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                        <fo:table-cell xsl:use-attribute-sets="table-cell-grid centered">
                            <fo:block>
                                <xsl:value-of select="//pn/number" />
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell xsl:use-attribute-sets="table-cell-grid green centered">
                            <fo:block>
                                <xsl:value-of select="number" />
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell xsl:use-attribute-sets="table-cell-grid green centered">
                            <fo:block>
                                <xsl:value-of select="revision" />
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell xsl:use-attribute-sets="table-cell-grid green centered">
                            <fo:block>
                                <xsl:value-of select="releasedBy" />
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell xsl:use-attribute-sets="table-cell-grid green centered">
                            <fo:block>
                                <xsl:value-of select="releaseDate" />
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                        <fo:table-cell xsl:use-attribute-sets="table-cell-grid table-inline-title">
                            <fo:block>Artikelbenennung / Part Description:</fo:block>
                        </fo:table-cell>
                        <fo:table-cell number-columns-spanned="4" xsl:use-attribute-sets="table-cell-grid">
                            <fo:block>
                                <xsl:value-of select="name" />
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>     
            
           
            <fo:block margin-top="80px">
                <fo:table table-layout="fixed" >
                    <fo:table-column column-width="22%" />
                    <fo:table-column column-width="78%" />
                    <fo:table-body>
                        <fo:table-row>
                            <fo:table-cell xsl:use-attribute-sets="table-cell-grid table-inline-title">
                                <fo:block>Info Service (SNN):</fo:block>
                                <fo:block>(for manual, part catalog):</fo:block>
                            </fo:table-cell>
                            <fo:table-cell xsl:use-attribute-sets="table-cell-grid blue">
                                <fo:block>
                                    <xsl:value-of select="//pn/infoService" />
                                </fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </fo:table-body>        
                </fo:table>
            </fo:block>
        
            <fo:block margin-top="15px">
                <fo:table table-layout="fixed" >
                    <fo:table-column column-width="22%" />
                    <fo:table-column column-width="78%" />
                    <fo:table-body>
                        <fo:table-row>
                            <fo:table-cell xsl:use-attribute-sets="table-cell-grid table-inline-title">
                                <fo:block>Info Marketing:</fo:block>
                                <fo:block>(for manual, part catalog):</fo:block>
                            </fo:table-cell>
                            <fo:table-cell xsl:use-attribute-sets="table-cell-grid blue">
                                <fo:block>
                                    <xsl:value-of select="//pn/infoMarketing" />
                                </fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </fo:table-body>        
                </fo:table>
            </fo:block>
        
            <fo:block margin-top="10px">
                <fo:table table-layout="fixed" >
                    <fo:table-column column-width="22%" />
                    <fo:table-column column-width="78%" />
                    <fo:table-body>
                        <fo:table-row>
                            <fo:table-cell xsl:use-attribute-sets="table-cell-grid table-inline-title">
                                <fo:block>Kurzbeschreibung / short reason:</fo:block>
                                <fo:block>(linked to drawing)</fo:block>
                            </fo:table-cell>
                            <fo:table-cell xsl:use-attribute-sets="table-cell-grid blue">
                                <fo:block>
                                    <xsl:value-of select="//pn/shortReason" />
                                </fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </fo:table-body>        
                </fo:table>
            </fo:block>
            
            <fo:block margin-top="15px">
                <fo:table table-layout="fixed" >
                    <fo:table-column column-width="22%" />
                    <fo:table-column column-width="78%" />
                    <fo:table-body>
                        <fo:table-row>
                            <fo:table-cell xsl:use-attribute-sets="table-cell-grid table-inline-title">
                                <fo:block>Ausführliche Beschreibung / reason:</fo:block>
                                <fo:block>(not linked to drawing)</fo:block>
                            </fo:table-cell>
                            <fo:table-cell xsl:use-attribute-sets="table-cell-grid blue">
                                <fo:block>
                                    <xsl:value-of select="//pn/reason" />
                                </fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </fo:table-body>        
                </fo:table>
            </fo:block>
            
            <fo:block margin-top="15px">
                <fo:table table-layout="fixed" >
                    <fo:table-column column-width="22%" />
                    <fo:table-column column-width="78%" />
                    <fo:table-body>
                        <fo:table-row>
                            <fo:table-cell xsl:use-attribute-sets="table-cell-grid table-inline-title">
                                <fo:block>Anlass / reason - source:</fo:block>
                                <fo:block>(not linked to drawing):</fo:block>
                            </fo:table-cell>
                            <fo:table-cell xsl:use-attribute-sets="table-cell-grid blue">
                                <fo:block>
                                    <xsl:value-of select="//pn/reasonSource" />
                                </fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </fo:table-body>        
                </fo:table>
            </fo:block>
        
            <fo:block margin-top="15px">
                <fo:table table-layout="fixed" >
                    <fo:table-column column-width="22%" />
                    <fo:table-column column-width="78%" />
                    <fo:table-body>
                        <fo:table-row>
                            <fo:table-cell xsl:use-attribute-sets="table-cell-grid table-inline-title">
                                <fo:block>Einfluss auf bestehende Teile / Impact to existing parts:</fo:block>
                                <fo:block>(not linked to drawing):</fo:block>
                            </fo:table-cell>
                            <fo:table-cell xsl:use-attribute-sets="table-cell-grid blue">
                                <fo:block>
                                    <xsl:value-of select="//pn/impactToExistingParts" />
                                </fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </fo:table-body>        
                </fo:table>
            </fo:block>
        
        </fo:block>
   
    </xsl:template>
    
  
</xsl:stylesheet>

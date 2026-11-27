<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Copyright (C) 2001-2016 Food and Agriculture Organization of the
  ~ United Nations (FAO-UN), United Nations World Food Programme (WFP)
  ~ and United Nations Environment Programme (UNEP)
  ~
  ~ This program is free software; you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation; either version 2 of the License, or (at
  ~ your option) any later version.
  ~
  ~ This program is distributed in the hope that it will be useful, but
  ~ WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  ~ General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with this program; if not, write to the Free Software
  ~ Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
  ~
  ~ Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
  ~ Rome - Italy. email: geonetwork@osgeo.org
  -->

<!--
  Used by the DCAT formatters of this plugin (dcat, eu-dcat-ap, eu-dcat-ap-hvd,
  eu-geodcat-ap), with ../rndt-dcat-identifier.xsl. What the DCAT formatters of
  the core lose of RNDT records (3: format names, below):

  1. Format of the distributions. In ISO 19139 the format is in
     MD_Distribution/distributionFormat; the iso19139 to ISO 19115-3 conversion
     leaves it there, while the DCAT formatters look for it in
     MD_DigitalTransferOptions/distributionFormat (dcat-core-distribution.xsl):
     no dct:format at all. In the conversion, every MD_DigitalTransferOptions
     without a format gets a copy of the formats of its MD_Distribution
     (distributionFormat is the last element of MD_DigitalTransferOptions).

  2. Owner, publisher, author. RNDT records give the owner of the resource as a
     citedResponsibleParty of the citation (role "owner"); the DCAT formatters
     only read the pointOfContact of the identification, so there is no
     dct:rightsHolder. The cited parties are converted with the same role
     table of the core (owner: dct:rightsHolder, publisher: dct:publisher,
     author: dct:creator, others: prov:qualifiedAttribution), points of contact
     excluded: they are already dcat:contactPoint.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:gmd="http://www.isotc211.org/2005/gmd"
                xmlns:mdb="http://standards.iso.org/iso/19115/-3/mdb/2.0"
                xmlns:mri="http://standards.iso.org/iso/19115/-3/mri/1.0"
                xmlns:mrd="http://standards.iso.org/iso/19115/-3/mrd/1.0"
                xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/2.0"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:dct="http://purl.org/dc/terms/"
                xmlns:dcat="http://www.w3.org/ns/dcat#"
                version="2.0"
                exclude-result-prefixes="#all">

  <xsl:template match="gmd:MD_DigitalTransferOptions" priority="5" mode="from19139to19115-3.2018">
    <mrd:MD_DigitalTransferOptions>
      <xsl:apply-templates select="@*|node()" mode="from19139to19115-3.2018"/>
      <xsl:apply-templates select="ancestor::gmd:MD_Distribution[1]/gmd:distributionFormat"
                           mode="from19139to19115-3.2018"/>
    </mrd:MD_DigitalTransferOptions>
  </xsl:template>

  <!--
    3. Format names of RNDT records missing from the table of the core
       ($formatLabelToUri, dcat-variables.xsl): "geo+json" and "x-shapefile"
       (names of the IANA and INSPIRE media types, without "application/") end
       up as a dct:format with a label only, no URI of the EU file types. They
       are looked up with the name the table knows; the label stays the original.
       Same template as the core (dcat-core-distribution.xsl) otherwise.
  -->
  <xsl:variable name="rndtFormatLabels" as="node()*">
    <entry key="application/geo+json">geo+json</entry>
    <entry key="shp">x-shapefile</entry>
    <entry key="shp">application/x-shapefile</entry>
  </xsl:variable>

  <xsl:template name="rdf-format-as-mediatype">
    <xsl:param name="elementName" as="xs:string" select="'dct:format'"/>
    <xsl:param name="format" as="xs:string"/>

    <xsl:variable name="lookup"
                  as="xs:string"
                  select="(($rndtFormatLabels[lower-case(text()) = lower-case($format)]/@key)[1], $format)[1]"/>
    <xsl:variable name="formatUri"
                  as="xs:string?"
                  select="($formatLabelToUri[lower-case($lookup) = lower-case(text())]/@key)[1]"/>

    <xsl:variable name="rangeName"
                  as="xs:string"
                  select="if ($elementName = 'dct:format') then 'dct:MediaTypeOrExtent' else 'dct:MediaType'"/>
    <xsl:element name="{$elementName}">
      <xsl:choose>
        <xsl:when test="$formatUri">
          <xsl:element name="{$rangeName}">
            <xsl:attribute name="rdf:about" select="$formatUri"/>
          </xsl:element>
        </xsl:when>
        <xsl:otherwise>
          <xsl:element name="{$rangeName}">
            <rdfs:label>
              <xsl:value-of select="$format"/>
            </rdfs:label>
          </xsl:element>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>

  <xsl:template mode="iso19115-3-to-dcat-resource" match="mdb:MD_Metadata" priority="10">
    <xsl:next-match/>
    <xsl:apply-templates mode="iso19115-3-to-dcat"
                         select="mdb:identificationInfo/*/mri:citation/*/cit:citedResponsibleParty
                                   [not(*/cit:role/*/@codeListValue = 'pointOfContact')]"/>
  </xsl:template>

</xsl:stylesheet>

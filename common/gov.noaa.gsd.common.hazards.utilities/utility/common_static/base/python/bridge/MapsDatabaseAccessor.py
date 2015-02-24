"""
This class will access the maps database.  The initial intended functionality 
is to retrieve geospatial data and associated metadata.  For example, dam break
inundation polygons and associated 'damInfo' metadata are stored in mapdata.daminfo
table.  This module will retrieve these data to be used in recommenders and 
product generation.

It is envisioned that this module can/will be extended to access similar information
like burn scar areas.   

 @since: February 2015
 @author: GSD Hazard Services Team
 
 History:
 Date         Ticket#    Engineer    Description
 ------------ ---------- ----------- --------------------------
 Feb 03, 2015            kmanross     Initial development

"""

from shapely.geometry import Polygon
from ufpy.dataaccess import DataAccessLayer
import DamMetaData

class MapsDatabaseAccessor(object):
    def __init__(self):
        pass
        
    def _extract_poly_coords(self, geom):
        if geom.type == 'Polygon':
            exterior_coords = geom.exterior.coords[:]
            interior_coords = []
            for int in geom.interiors:
                interior_coords += int.coords[:]
        elif geom.type == 'MultiPolygon':
            exterior_coords = []
            interior_coords = []
            for part in geom:
                epc = self._extract_poly_coords(part)  # Recursive call
                exterior_coords += epc['exterior_coords']
                interior_coords += epc['interior_coords']
        else:
            raise ValueError('Unhandled geometry type: ' + repr(geom.type))
        return {'exterior_coords': exterior_coords,
                'interior_coords': interior_coords}
        
    def getPolygonNames(self, tablename):
        
        table = "mapdata."+tablename
        req = DataAccessLayer.newDataRequest()
        req.setDatatype("maps")
        req.addIdentifier("table", table)
        req.addIdentifier("geomField", "the_geom")
        
        columns = [
                   "name",
                   ]

        ### Important to set addIdentifier("locationField", "name") !!!
        req.addIdentifier("locationField", columns[0])
        req.setParameters(*columns)
        
        retGeoms = DataAccessLayer.getGeometryData(req)
        
        nameList = []
        for retGeom in retGeoms :
            nameList.append(retGeom.getString('name'))
            
        return nameList

        
    
    def getPolygonDict(self, tablename):
        
        table = "mapdata."+tablename
        req = DataAccessLayer.newDataRequest()
        req.setDatatype("maps")
        req.addIdentifier("table", table)
        req.addIdentifier("geomField", "the_geom")
        
        columns = [
                   "name",
                   ]

        ### Important to set addIdentifier("locationField", "name") !!!
        req.addIdentifier("locationField", columns[0])
        req.setParameters(*columns)
        
        retGeoms = DataAccessLayer.getGeometryData(req)
        
        retDict = {}
        for retGeom in retGeoms :
            poly = self._extract_poly_coords(retGeom.getGeometry())
            formattedPoly = [list(c) for c in poly['exterior_coords']]
            
            try:
                id = retGeom.getString('name')
                if id:
                   retDict[id] = formattedPoly
                
            except:
                print "No 'name' column in table:", table
            
        return retDict

    
    def getDamInundationMetadata(self, damName):
        damInundationMetadata = DamMetaData.damInundationMetadata
        return damInundationMetadata.get(damName)
    
    def getAllDamInundationMetadata(self):
        return DamMetaData.damInundationMetadata
    
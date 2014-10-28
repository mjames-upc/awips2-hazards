from ufpy.dataaccess import DataAccessLayer
'''
    Description: Utility class to retrieve points from the map database.
    
    SOFTWARE HISTORY
    Date         Ticket#    Engineer    Description
    ------------ ---------- ----------- --------------------------
    Oct 22, 2014    3572    jsanchez    Initial creation   
    
    @version 1.0
'''

def retrievePoints(geometryCollection, tablename, constraints=None, sortBy=None):
    """
    Returns the list of location names.
    @param geometryCollection: the geometry that will be used to intersect the points
    @param tablename: the name of the mapdata table to query against
    @param constraints: a dictionary that has a table field name map to a value or a list of values
    @param sortBy: a list of table field names to sort the results by
    @return: Returns the list of location names.
    """ 
    params = set() 
    if constraints and type(constraints) is dict:
        params.update(constraints.keys())
    
    if sortBy and type(sortBy) is list:
        params.update(sortBy)
        
    req = DataAccessLayer.newDataRequest('maps', parameters=list(params))
    req.addIdentifier('table','mapdata.' + tablename)
    req.addIdentifier('geomField','the_geom')
    req.addIdentifier('locationField', 'name')    
    
    locations = []
    potentialGeometryData = []
    for geom in geometryCollection:
        req.setEnvelope(geom.envelope)
        geometryData = DataAccessLayer.getGeometryData(req)
        for data in geometryData:
            if geom.intersects(data.getGeometry()):
                potentialGeometryData.append(data)
         
    # collect only the geometries that intersect the geoms
    # and fits the constraints
    validGeometryData = []    
    for potential in potentialGeometryData:
        if constraints and type(constraints) is dict:
            for constraint in constraints:
                value = constraints[constraint]
                if type(value) is list and potential.getString(constraint) in value:
                    validGeometryData.append(potential)
                elif potential.getString(constraint) != value:
                    validGeometryData.append(potential)
    
    results = []
    for valid in validGeometryData:
        result = [valid.getLocationName()]
        if sortBy and type(sortBy) is list:
            for param in sortBy:
                result.append(valid.getString(param))
        results.append(result)
              
    # order the results
    if sortBy and type(sortBy) is list:
        results.sort(key=lambda x: tuple(x[i] for i in range(1, len(sortBy)+1)))           

    return [result[0] for result in results]

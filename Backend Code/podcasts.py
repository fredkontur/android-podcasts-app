###############################################################################
# Filename:     podcasts.py
# Author:       Fred Kontur
# Date written: November 24, 2017
# Last Edited:  November 25, 2017
# Description:  This program creates an API for creating playlists of
#               podcasts. It also implements user accounts for ownership of
#               the playlists. The initial login takes place on the front end,
#               with backend authorization in this program to ensure that the
#               request comes from an authorized source.
###############################################################################
from google.appengine.ext import ndb
from google.oauth2 import id_token
from google.auth.transport import requests
import webapp2
import json

# Enter your client ID from Google below
# You can get a Google client ID her https://developers.google.com/identity/sign-in/web/sign-in
CLIENT_ID = 'YOUR_CLIENT_ID'

class Playlist(ndb.Model):
    name = ndb.StringProperty(required = True)
    description = ndb.StringProperty()
    podcastList = ndb.StringProperty(repeated = True)
    endorsements = ndb.StringProperty(repeated = True)
    comments = ndb.StringProperty(repeated = True)
    ownerName = ndb.StringProperty(default = None)
    ownerID = ndb.StringProperty(default = None)
    
class Podcast(ndb.Model):
    title = ndb.StringProperty(required = True)
    description = ndb.StringProperty()
    genre = ndb.StringProperty()
    numEpisodes = ndb.IntegerProperty()
    numSeasons = ndb.IntegerProperty()
    active = ndb.BooleanProperty()
    ratings = ndb.IntegerProperty(repeated = True)
    playlists = ndb.StringProperty(repeated = True)

class PlaylistHandler(webapp2.RequestHandler):
    def post(self):
    ###########################################################################
    # Parameters:   self  The instance of the PlaylistHandler class
    # Returns:      Nothing
    # Description:  This method creates a new playlist object.
    ###########################################################################
        # Create a parent playlist and make all of the other playlist instances
        # ancestors of the parent playlist
        parentPlaylistKey = ndb.Key(Playlist, "parentPlaylist")
        playlistData = json.loads(self.request.body)
        
        # Make sure the data passed to the method is valid
        status = checkPlaylistData(None, playlistData, "create", self)
        
        if status != 200:
            self.response.status_int = status
        # If the data is valid, create the new playlist with the data passed
        else:
            newPlaylist = Playlist(name = playlistData['name'], parent = parentPlaylistKey)
            # Use the setattr() method to set the attributes in the playlist
            # except for the podcast list
            for attr, val in playlistData.iteritems():
                if(attr != 'podcastList'): 
                    setattr(newPlaylist, attr, val)
            # If a podcastList has been passed, iterate through the list and
            # add them to the playlist one at a time
            if 'podcastList' in playlistData:
                for podcast in playlistData['podcastList']:
                    newPlaylist.podcastList.append(podcast)
            newPlaylist.put()
            playlistID = newPlaylist.key.urlsafe()
            # If a podcastList has been passed, update the "playlist"
            # attribute of the affected podcast
            if 'podcastList' in playlistData:
                pcList = playlistData['podcastList']
                updatePodcastPlaylists(playlistID, None, pcList)
            # Output the data for the new playlist to the user
            playlistDict = newPlaylist.to_dict()
            playlistDict['id'] = playlistID
            playlistDict['self'] = '/playlists/' + playlistID
            self.response.status_int = 201
            self.response.write(json.dumps(playlistDict))
        
    def get(self, id = None):
    ###########################################################################
    # Parameters:   self  The instance of the PlaylistHandler class
    #               id    The id of the playlist, passed in via the URL
    # Returns:      Nothing
    # Description:  This method returns to the caller the data for the 
    #               playlist
    ###########################################################################
        # First, check to see if an id was passed in
        if id:
            # If the playlist can be found, return it's data. Otherwise, return an
            # error message.
            try:
                currPlaylist = ndb.Key(urlsafe = id).get()
                playlistDict = currPlaylist.to_dict()
                playlistDict['self'] = '/playlists/' + id
                self.response.write(json.dumps(playlistDict))
            except:
                self.response.status_int = 400
                self.response.write("Invalid playlist ID")
        else:
            self.response.status_int = 400
            self.response.write("Need to supply playlist ID in URL")

    def delete(self, id = None):
    ###########################################################################
    # Parameters:   self  The instance of the PlaylistHandler class
    #               id    The id of the playlist, passed in via the URL
    # Returns:      Nothing
    # Description:  This method deletes the playlist passed to it.
    ###########################################################################
        # First, check to see if an id was passed in
        if id:
            # If an idToken was passed in the header, then store it in the 
            # idToken variable
            try:
                idToken = self.request.headers['Authorization']
            except:
                idToken = None
            # If the playlist can be found and the user is authorized, then
            # delete it. Otherwise, return an error message.
            try:
                currPlaylist = ndb.Key(urlsafe = id).get()
                if isAuthorized(currPlaylist, None, idToken):
                    # Remove playlist from the podcasts' lists
                    currPodcastList = currPlaylist.podcastList
                    updatePodcastPlaylists(id, currPodcastList, None)
                    # Delete the playlist
                    ndb.Key(urlsafe = id).delete()
                    self.response.status_int = 204
                else:
                    self.response.status_int = 403
            except:
                self.response.status_int = 400
                self.response.write("Invalid playlist ID")
        else:
            self.response.status_int = 400
            self.response.write("Need to supply playlist ID in URL")

    def patch(self, id = None):
    ###########################################################################
    # Parameters:  self  The instance of the PlaylistHandler class
    #              id    The id of the playlist, passed in via the URL
    # Returns:     Nothing
    # Description: This method changes the playlist data based on the data
    #              passed to it via the message body.
    ###########################################################################
        # First, check to see if an id was passed in
        if id:
            # If an idToken was passed in the header, then store it in the 
            # idToken variable
            try:
                idToken = self.request.headers['Authorization']
            except:
                idToken = None
            # If the playlist can be found and the user is authorized, then
            # change its data based on the data that was passed in through the 
            # message body
            try:
                currPlaylist = ndb.Key(urlsafe = id).get()
                playlistData = json.loads(self.request.body)
                if isAuthorized(currPlaylist, playlistData, idToken):
                    playlistData = json.loads(self.request.body)
                    # Make sure that the passed-in data is valid
                    status = checkPlaylistData(id, playlistData, "modify", self)
                    # If the data is valid, edit the playlist's data. If not,
                    # send an error message.
                    if status != 200:
                        self.response.status_int = status
                    else:
                        updatePlaylist(id, currPlaylist, playlistData)
                    self.response.status_int = 204
                else:
                    self.response.status_int = 405
            except:
                self.response.status_int = 400
                self.response.write("Invalid playlist ID")
        else:
            self.response.status_int = 400
            self.response.write("Need to supply playlist ID in URL")
    
    def put(self, id = None):
    ###########################################################################
    # Parameters:  self  The instance of the PlaylistHandler class
    #              id    The id of the playlist, passed in via the URL
    # Returns:     Nothing
    # Description: This method replaces the playlist with another one using the
    #              data passed to it via the message body.
    ###########################################################################
        # First, check to see if an id was passed in
        if id:
            # If an idToken was passed in the header, then store it in the 
            # idToken variable
            try:
                idToken = self.request.headers['Authorization']
            except:
                idToken = None
            # If the playlist can be found and the user is authorized, then 
            # replace the playlist with the new one using the data that was 
            # passed in through the message body
            try:
                currPlaylist = ndb.Key(urlsafe = id).get()
                if isAuthorized(currPlaylist, None, idToken):
                    playlistData = json.loads(self.request.body)
                    # Make sure that the passed-in data is valid
                    status = checkPlaylistData(id, playlistData, "replace", self)
                    
                    # If the data is valid, replace the playlist. If not, send an 
                    # error message.
                    if status != 200:
                        self.response.status_int = status
                    else:
                        clearPlaylistData(id, currPlaylist)
                        updatePlaylist(id, currPlaylist, playlistData)
                        self.response.status_int = 204
                else:
                    self.response.status_int = 403
            except:
                self.response.status_int = 400
                self.response.write("Invalid playlist ID")
        else:
            self.response.status_int = 400
            self.response.write("Need to supply playlist ID in URL")

class MultPlaylistsHandler(webapp2.RequestHandler):
    def get(self):
    ###########################################################################
    # Parameters:   self  The instance of the MultPlaylistsHandler class
    # Returns:      Nothing
    # Description:  This method returns the data for all of the playlist
    #               objects that have been instatiated and not deleted.
    ###########################################################################
        # Run a query to get all playlists
        allPlaylists = Playlist.query()
        # Need to format the output so that it is properly recognized as a
        # a list of JSON objects
        self.response.write('[\n')
        n = 0
        # Iterate through the list of playlists and output the data for each
        for currPlaylist in allPlaylists:
            if n > 0:
                self.response.write(',\n')
            n = n + 1
            playlistDict = currPlaylist.to_dict()
            playlistDict['id'] = currPlaylist.key.urlsafe()
            playlistDict['self'] = '/playlists/' + currPlaylist.key.urlsafe()
            self.response.write('\t' + json.dumps(playlistDict))
        self.response.write('\n]')

class PodcastsInPlaylistHandler(webapp2.RequestHandler):
    def get(self, id = None):
    ###########################################################################
    # Parameters:   self  The instance of the PodcastsInPlaylistHandler class
    #               id    The id of the playlist, passed in via the URL
    # Returns:      Nothing
    # Description:  This method returns to the caller the data for the
    #               podcasts included in the playlist whose id is passed in
    ###########################################################################
        # First, check to see if an id was passed in
        if id:
            # If the playlist can be found, then return its podcasts
            try:
                currPlaylist = ndb.Key(urlsafe = id).get()
                podcastList = currPlaylist.podcastList
                self.response.write('[\n')
                n = 0
                # Iterate through the list of podcasts and output the data for each
                for currPodcastID in podcastList:
                    if n > 0:
                        self.response.write(',\n')
                    pc = ndb.Key(urlsafe = currPodcastID).get()
                    n = n + 1
                    podcastDict = pc.to_dict()
                    podcastDict['id'] = pc.key.urlsafe()
                    podcastDict['self'] = '/podcasts/' + pc.key.urlsafe()
                    self.response.write('\t' + json.dumps(podcastDict))
                self.response.write('\n]')
            except:
                self.response.status_int = 400
                self.response.write("Invalid playlist ID")
        else:
            self.response.status_int = 400
            self.response.write("Need to supply playlist ID in URL")


class PodcastHandler(webapp2.RequestHandler):
    def post(self):
    ###########################################################################
    # Parameters:   self  The instance of the PodcastHandler class
    # Returns:      Nothing
    # Description:  This method creates a new podcast object.
    ###########################################################################
        # Create a parent podcast and make all of the other podcast instances
        # ancestors of the parent podcast
        parentPodcastKey = ndb.Key(Podcast, "parentPodcast")
        podcastData = json.loads(self.request.body)
        
        # Make sure the data passed to the method is valid
        status = checkPodcastData(None, podcastData, "create", self)
        
        if status != 200:
            self.response.status_int = status
        # If the data is valid, create the new podcast with the data passed
        else:
            newPodcast = Podcast(title = podcastData['title'], parent = parentPodcastKey)
            for attr, val in podcastData.iteritems():
                setattr(newPodcast, attr, val)
            newPodcast.put()
            # Output the data for the new playlist to the user
            podcastDict = newPodcast.to_dict()
            podcastDict['id'] = newPodcast.key.urlsafe()
            podcastDict['self'] = '/podcasts/' + newPodcast.key.urlsafe()
            self.response.status_int = 201
            self.response.write(json.dumps(podcastDict))
        
    def get(self, id = None):
    ###########################################################################
    # Parameters:   self  The instance of the PodcastHandler class
    #               id    The id of the podcas, passed in via the URL
    # Returns:      Nothing
    # Description:  This method returns to the caller the data for the 
    #               podcast
    ###########################################################################
        # First, check to see if an id was passed in
        if id:
            # If the podcast can be found, return it's data. Otherwise, return an
            # error message.
            try:
                currPodcast = ndb.Key(urlsafe = id).get()
                podcastDict = currPodcast.to_dict()
                podcastDict['self'] = '/podcasts/' + id
                self.response.write(json.dumps(podcastDict))
            except:
                self.response.status_int = 400
                self.response.write("Invalid podcast ID")
        else:
            self.response.status_int = 400
            self.response.write("Need to supply podcast ID in URL")

    def delete(self, id = None):
    ###########################################################################
    # Parameters:   self  The instance of the PodcastHandler class
    #               id    The id of the podcast, passed in via the URL
    # Returns:      Nothing
    # Description:  This method deletes the podcast passed to it.
    ###########################################################################
        # First, check to see if an id was passed in
        if id:
            # If the podcast can be found, then delete it. Otherwise, return
            # an error message
            try: 
                currPodcast = ndb.Key(urlsafe = id).get()
                # Clear the podcast data to remove podcast from playlists
                clearPodcastData(id, currPodcast)
                # Delete the podcast
                ndb.Key(urlsafe = id).delete()
                self.response.status_int = 204
            except:
                self.response.status_int = 400
                self.response.write("Invalid playlist ID")
        else:
            self.response.status_int = 400
            self.response.write("Need to supply playlist ID in URL")

    def patch(self, id = None):
    ###########################################################################
    # Parameters:  self  The instance of the PodcastHandler class
    #              id    The id of the podcast, passed in via the URL
    # Returns:     Nothing
    # Description: This method changes the podcast data based on the data
    #              passed to it via the message body.
    ###########################################################################
        # First, check to see if an id was passed in
        if id:
            # If the podcast can be found, then change its data based on the 
            # data that was passed in through the message body 
            try:
                currPodcast = ndb.Key(urlsafe = id).get()
                podcastData = json.loads(self.request.body)
                
                # Make sure that the passed-in data is valid
                status = checkPodcastData(id, podcastData, "modify", self)
                
                # If the data is valid, edit the playlist's data. If not, send
                # an error message.
                if status != 200:
                    self.response.status_int = status
                else:
                    updatePodcast(id, currPodcast, podcastData)
                    self.response.status_int = 204
            except:
                self.response.status_int = 400
                self.response.write("Invalid podcast ID")
        else:
            self.response.status_int = 400
            self.response.write("Need to supply podcast ID in URL")
    
    def put(self, id = None):
    ###########################################################################
    # Parameters:  self  The instance of the PodcastHandler class
    #              id    The id of the podcast, passed in via the URL
    # Returns:     Nothing
    # Description: This method replaces the podcast with another one using the
    #              data passed to it via the message body.
    ###########################################################################
        # First, check to see if an id was passed in
        if id:
            # If the podcast can be found, then replace the podcast with the
            # one using the data that was passed in through the message body 
            try:
                currPodcast = ndb.Key(urlsafe = id).get()
                podcastData = json.loads(self.request.body)
                
                # Make sure that the passed-in data is valid
                status = checkPodcastData(id, podcastData, "replace", self)
                
                # If the data is valid, replace the playlist. If not, send an 
                # error message.
                if status != 200:
                    self.response.status_int = status
                else:
                    clearPodcastData(id, currPodcast)
                    updatePodcast(id, currPodcast, podcastData)
                    self.response.status_int = 204
            except:
                self.response.status_int = 400
                self.response.write("Invalid podcast ID")
        else:
            self.response.status_int = 400
            self.response.write("Need to supply podcast ID in URL")

class MultPodcastsHandler(webapp2.RequestHandler):
    def get(self):
    ###########################################################################
    # Parameters:   self  The instance of the MultPodcastsHandler class
    # Returns:      Nothing
    # Description:  This method returns the data for all of the podcast
    #               objects that have been instatiated and not deleted.
    ###########################################################################
        # Run a query to get all podcasts
        allPodcasts = Podcast.query()
        # Need to format the output so that it is properly recognized as a
        # a list of JSON objects
        self.response.write('[\n')
        n = 0
        # Iterate through the list of podcasts and output the data for each
        for currPodcast in allPodcasts:
            if n > 0:
                self.response.write(',\n')
            n = n + 1
            podcastDict = currPodcast.to_dict()
            podcastDict['id'] = currPodcast.key.urlsafe()
            podcastDict['self'] = '/podcasts/' + currPodcast.key.urlsafe()
            self.response.write('\t' + json.dumps(podcastDict))
        self.response.write('\n]')

        
class PlaylistsContainingPodcastHandler(webapp2.RequestHandler):
    def get(self, id = None):
    ###########################################################################
    # Parameters:   self  The instance of the PlaylistsContaingPodcastHandler 
    #                     class
    #               id    The id of the playlist, passed in via the URL
    # Returns:      Nothing
    # Description:  This method returns to the caller the data for the
    #               playlists that have included the podcast whose id is 
    #               passed in
    ###########################################################################
        # First, check to see if an id was passed in
        if id:
            # If the podcast can be found, then return the playlists
            # containing it
            try:
                currPodcast = ndb.Key(urlsafe = id).get()
                plList = currPodcast.playlists
                self.response.write('[\n')
                n = 0
                # Iterate through the playlists and output the data for each
                for currPlaylistID in plList:
                    if n > 0:
                        self.response.write(',\n')
                    pl = ndb.Key(urlsafe = currPlaylistID).get()
                    n = n + 1
                    playlistDict = pl.to_dict()
                    playlistDict['id'] = pl.key.urlsafe()
                    playlistDict['self'] = '/playlists/' + pl.key.urlsafe()
                    self.response.write('\t' + json.dumps(playlistDict))
                self.response.write('\n]')
            except:
                self.response.status_int = 400
                self.response.write("Invalid podcast ID")
        else:
            self.response.status_int = 400
            self.response.write("Need to supply podcast ID in URL")
            

def checkPlaylistData(id, playlistData, action, handler):
###############################################################################
# Parameters:  id            The id of the playlist
#              playlistData  The data passed in with the request
#              action        What action - "create", "replace", or "modify" -
#                            should be done with the data
#              handler       The handler object that has been called to take
#                            action on the playlist
# Returns:     An integer representing the status code
# Description: This function checks the data passed in for a playlist to
#              ensure that it is valid
###############################################################################
    status = 200
    
    # The only fields present in the data should be name, description, 
    # podcastList, endorsements, comments, ownerName, and ownerID. If any 
    # other field is present, then send an error message
    for playlistAttribute in playlistData:
        if playlistAttribute == 'name':
            pass
        elif playlistAttribute == 'description':
            pass
        elif playlistAttribute == 'podcastList':
            pass
        elif playlistAttribute == 'endorsements':
            pass
        elif playlistAttribute == 'comments':
            pass
        elif playlistAttribute == 'ownerName':
            pass
        elif playlistAttribute == 'ownerID':
            pass
        # new_endorsement and new_comment can be passed for modify
        elif playlistAttribute == 'newEndorsement' and action == 'modify':
            pass
        elif playlistAttribute == 'newComment' and action == 'modify':
            pass
        else:
            status = 400
            errMsgStart = '"' + playlistAttribute + '"'
            errMsgTot = errMsgStart + " is not a playlist Attribute"
            handler.response.write(errMsgTot)
            return status
        
    # If a podcastList is provided, make sure that it has existing 
    # podcasts
    if 'podcastList' in playlistData:
        for pc in playlistData['podcastList']:
            currPodcast = ndb.Key(urlsafe = pc).get()
            if not currPodcast:
                status = 400
                errMsg = 'Podcast "' + pc +'" does not exist'
                handler.response.write(errMsg)
                return status
        
        
    if action == "create":
        # name is a required field
        if 'name' not in playlistData:
            status = 400
            errMsg = '"name" is required for playlist creation'
            handler.response.write(errMsg)
            return status
        # make sure the name is not being used by another playlist
        else:
            currName = playlistData['name']
            checkPlaylists = Playlist.query(Playlist.name == currName).get()
            if checkPlaylists:
                status = 403
                errMsg = 'Playlist name "' + currName + '" is already taken'
                handler.response.write(errMsg)
                return status
        # endorsements and comments should not be passed at creation
        if 'endorsements' in playlistData or 'comments' in playlistData:
            status = 403
            errMsgStart = '"endorsements" and/or "comments" should not be '
            errMsgTot = errMsgStart + 'passed at playlist creation'
            handler.response.write(errMsgTot)
            return status
        
    elif action == "modify":
    # If a name was passed, check it to make sure it is not already being used
    # by another playlist
        if 'name' in playlistData:
            replName = playlistData['name']
            currPlaylist = ndb.Key(urlsafe = id).get()
            if replName != currPlaylist.name:
                checkPlaylists = Playlist.query(Playlist.name == replName).get()
                if checkPlaylists:
                    status = 403
                    errMsg = 'Playlist name "' + replName + '" is already taken'
                    handler.response.write(errMsg)
                    return status
    
    # "replace" case
    else:
        # name is a required field
        if 'name' not in playlistData:
            status = 400
            errMsg = '"name" is required for playlist creation'
            handler.response.write(errMsg)
            return status
        # make sure the name is not being used by another playlist
        replName = playlistData['name']
        currPlaylist = ndb.Key(urlsafe = id).get()
        if replName != currPlaylist.name:
            checkPlaylists = Playlist.query(Playlist.name == replName).get()
            if checkPlaylists:
                status = 403
                errMsg = 'Playlist name "' + currName + '" is already taken'
                handler.response.write(errMsg)
                return status
        # endorsements and comments should not be passed at replacement
        if 'endorsements' in playlistData or 'comments' in playlistData:
            status = 403
            errMsgStart = '"endorsements" and/or "comments" should not be '
            errMsgTot = errMsgStart + 'passed at playlist replacement'
            handler.response.write(errMsgTot)
            return status
        
    return status


def checkPodcastData(id, podcastData, action, handler):
###############################################################################
# Parameters:  id           The id of the podcast
#              podcastData  The data passed in with the request
#              action       What action - "create", "replace", or "modify" -
#                           should be done with the data
#              handler      The handler object that has been called to take
#                           action on the podcast
# Returns:     An integer representing the status code
# Description: This function checks the data passed in for a podcast to
#              ensure that it is valid
###############################################################################
    status = 200
    
    # The only fields present in the data should be title, description, genre, 
    # numEpisodes, numSeasons, active, ratings and playlists. If any other
    # field is present, then send an error message
    for podcastAttribute in podcastData:
        if podcastAttribute == 'title':
            pass
        elif podcastAttribute == 'description':
            pass
        elif podcastAttribute == 'genre':
            pass
        elif podcastAttribute == 'numEpisodes':
            pass
        elif podcastAttribute == 'numSeasons':
            pass
        elif podcastAttribute == 'active':
            pass
        elif podcastAttribute == 'ratings':
            pass
        elif podcastAttribute == 'playlists':
            pass
        # 'new_rating' can be passed as an attribute only for 'modify'
        elif podcastAttribute == 'newRating' and action == 'modify':
            pass
        else:
            status = 400
            errMsgStart = '"' + podcastAttribute + '"'
            errMsgTot = errMsgStart + " is not a podcast Attribute"
            handler.response.write(errMsgTot)
            return status
    
    # The modification of the playlists field in podcasts should be initiated
    # by a creation, modification, or replacement a Playlist object, not by a
    # call to a Podcast object.
    if 'playlists' in podcastData:
        status = 403
        errMsgStart = 'Podcast "playlists" attribute can only be changed by '
        errMsgTot = errMsgStart + 'Playlist object, not directly by the user'
        handler.response.write(errMsgTot)
        return status
    
    # If numEpisodes, numSeasons, or ratings are passed, make sure they are
    # passed as  integers
    if 'numEpisodes' in podcastData:
        if not isinstance(podcastData['numEpisodes'], int):
            status = 400
            errMsg = '"numEpisodes" must be an integer value'
            handler.response.write(errMsg)
            return status
    if 'numSeasons' in podcastData:
        if not isinstance(podcastData['numSeasons'], int):
            status = 400
            errMsg = '"numSeasons" must be an integer value'
            handler.response.write(errMsg)
            return status
    if 'new_rating' in podcastData:
        if not isinstance(podcastData['new_rating'], int):
            status = 400
            errMsg = '"new_rating" must be an integer value'
            handler.response.write(errMsg)
            return status
    if 'ratings' in podcastData:
        for r in podcastData['ratings']:
            if not isinstance(r, int):
                status = 400
                errMsg = '"ratings" must be integers'
                handler.response.write(errMsg)
                return status
            
    # If active is passed, make sure it is passed as a boolean
    if 'active' in podcastData:
        if not isinstance(podcastData['active'], bool):
            status = 400
            errMsg = '"active" must be a boolean'
            handler.response.write(errMsg)
            return status
        
    if action == "create":
        # title is a required field
        if 'title' not in podcastData:
            status = 400
            errMsg = '"title" is required for podcast creation'
            handler.response.write(errMsg)
            return status
        # ratings should not be passed at creation
        if 'ratings' in podcastData:
            status = 403
            errMsg = '"ratings" should not be passed at podcast creation'
            handler.response.write(errMsg)
            return status
    
    # Right now, no further data checks are needed for the modify case
    elif action == "modify":
        pass
    
    # "Replace" case
    else:
        # title is a required field
        if 'title' not in podcastData:
            status = 400
            errMsg = '"title" is required for podcast replacement'
            handler.response.write(errMsg)
            return status
        # ratings should not be passed at replacemnt
        if 'ratings' in podcastData:
            status = 403
            errMsg = '"ratings" should not be passed at podcast replacement'
            handler.response.write(errMsg)
            return status
    
    return status


def updatePlaylist(id, mPlaylist, playlistData):
###############################################################################
# Parameters:  id            The id of the playlist
#              mPlaylist     The playlist being updated
#              playlistData  The data passed in with the request
# Returns:     Nothing
# Description: This function updates the playlist with the data passed. Only
#              the fields in playlistData are updated.
###############################################################################
    origPlaylist = mPlaylist.podcastList
    # Set the attributes in the playlist
    if 'title' in playlistData:
        mPlaylist.title = playlistData['title']
    if 'description' in playlistData:
        mPlaylist.description = playlistData['description']
    if 'ownerName' in playlistData:
        mPlaylist.ownerName = playlistData['ownerName']
    if 'ownerID' in playlistData:
        mPlaylist.ownerID = playlistData['ownerID']
    if 'newEndorsement' in playlistData:
        mPlaylist.endorsements.append(playlistData['newEndorsement'])
    if 'newComment' in playlistData:
        mPlaylist.comments.append(playlistData['newComment'])
    # For the podcastList, reset the podcastList to an empty list and then
    # rebuild the list one element at a time
    if 'podcastList' in playlistData:
        mPlaylist.podcastList = []
        for podcast in playlistData['podcastList']:
            mPlaylist.podcastList.append(podcast)
    # Update the "playlists" attribute in the appropriate podcasts to reflect
    # the changes in the playlist's podcastList
        currPlaylist = playlistData['podcastList']
        updatePodcastPlaylists(id, origPlaylist, currPlaylist)
    mPlaylist.put()

def updatePodcast(id, mPodcast, podcastData):
###############################################################################
# Parameters:  id           The id of the podcast
#              mPodcast     The podcast being updated
#              podcastData  The data passed in with the request
# Returns:     Nothing
# Description: This function updates the podcast with the data passed. Only
#              the fields in podcastData are updated.
###############################################################################
    # Set the attributes in the podcast
    for attr, val in podcastData.iteritems():
        if attr == 'newRating':
            mPodcast.ratings.append(val)
        else:
            setattr(mPodcast, attr, val)
    mPodcast.put()

def clearPlaylistData(id, mPlaylist):
###############################################################################
# Parameters:  id            The id of the playlist
#              mPlaylist     The playlist being updated
# Returns:     Nothing
# Description: This function clears all of the data fields in the playlist
###############################################################################
    mPlaylist.description = None
    mPlaylist.ownerName = None
    mPlaylist.ownerID = None
    mPlaylist.endorsements = []
    mPlaylist.comments = []
    # If there is a podcastList, use the updatePodcastPlaylists() function to 
    # update the podcast playlist data
    if mPlaylist.podcastList:
        updatePodcastPlaylists(id, mPlaylist.podcastList, None)
        mPlaylist.podcastList = []
    mPlaylist.put()

def clearPodcastData(id, mPodcast):
###############################################################################
# Parameters:  id            The id of the podcast
#              mPlaylist     The podcast being updated
# Returns:     Nothing
# Description: This function clears all of the data fields in the podcast
###############################################################################
    mPodcast.description = None
    mPodcast.genre = None
    mPodcast.numEpisodes = None
    mPodcast.numSeasons = None
    mPodcast.active = None
    mPodcast.ratings = []
    # If there are playlists, use the removePodcastFromPlaylists() function to
    # remove the podcast from all of the playlists
    if mPodcast.playlists:
        removePodcastFromPlaylists(id, mPodcast.playlists)
        mPodcast.playlists = []
    mPodcast.put()

def updatePodcastPlaylists(playlistID, origPodcastList, currPodcastList):
###############################################################################
# Parameters:  playlistID       The id of the playlist
#              origPodcastList  The original list of podcasts for the playlist
#                               before the latest changes
#              currPodcastList  The current podcast list after the changes
#                               have been made
# Returns:     Nothing
# Description: This function updates the playlists attribute of any affected
#              podcasts after an update to the podcastPlaylist attribute of a
#              playlist
###############################################################################
    # Determine which podcasts need to be added or removed
    podcastsToAdd = []
    podcastsToRemove = []
    # Determine which podcasts need to have the playlist added or removed by 
    # calculating the difference in the origPodcastList and currPodcastList
    if origPodcastList and currPodcastList:
        podcastsToRemove = list(set(origPodcastList) - set(currPodcastList))
        podcastsToAdd = list(set(currPodcastList) - set(origPodcastList))
    elif origPodcastList:
        podcastsToRemove = origPodcastList
    else:
        podcastsToAdd = currPodcastList
    
    # Remove the playlist from the podcasts that it no longer has on its list
    if podcastsToRemove:
        for podcastID in podcastsToRemove:
            currPodcast = ndb.Key(urlsafe = podcastID).get()
            currPodcast.playlists.remove(playlistID)
            currPodcast.put()
    
    # Add the playlist to podcasts that have been added to the playlist
    if podcastsToAdd:
        for podcastID in podcastsToAdd:
            currPodcast = ndb.Key(urlsafe = podcastID).get()
            currPodcast.playlists.append(playlistID)
            currPodcast.put()
    
    
def removePodcastFromPlaylists(podcastID, playlists):
###############################################################################
# Parameters:  podcastID  The id of the podcast
#              playlists  The playlists associated with the podcast
# Returns:     Nothing
# Description: This function removes a podcast from affected playlists after a
#              deletion or replacement of the podcast has occurred.
###############################################################################
    for playlistID in playlists:
        currPlaylist = ndb.Key(urlsafe = playlistID).get()
        currPlaylist.podcastList.remove(podcastID)
        currPlaylist.put()

    
def isAuthorized(mPlaylist, playlistData, token):
###############################################################################
# Parameters:  mPlaylist     The playlist that is being modified, replaced, or 
#                            deleted
#              playlistData  The data that has been passed for the playlist
#              token        The Google ID token used to confirm that the user
#                           has given permission to make the request changes
#                           to the playlist
# Returns:     Nothing
# Description: This function uses an ID token to confirm that a user has
#              signed into the trusted app to make the requested changes to
#              the playlist. This prevents someone from sending fake requests
#              to make changes to user-owned playlists.
# Source:      The Google developers site discussion of authentication with a
#              backend server provided Python code that was used to write this
#              function - https://developers.google.com/identity/sign-in/android/backend-auth
###############################################################################
    authStatus = True
    # check to see if the playlist has an ownerID
    # if no ownerID, anyone is authorized to edit or delete the playlist
    if not mPlaylist.ownerID:
        return authStatus
    # check to see if the user is trying to make a comment or endorsement,
    # which any user is authorized to do
    if playlistData is not None:
        newCommentOrEndorsement = True
        for attr, val in playlistData.iteritems():
            if attr != 'newComment' and attr != 'newEndorsement':
                newCommentOrEndorsement = False
                break
        if newCommentOrEndorsement:
            return authStatus
    try:
        idinfo = id_token.verify_oauth2_token(token, requests.Request(), CLIENT_ID)
        if idinfo['iss'] not in ['accounts.google.com', 'https://accounts.google.com']:
            raise ValueError('Wrong issuer.')
            authStatus = False
        else:
            # ID token is valid. Get the user's Google Account ID from the decoded token.
            userID = str(idinfo['sub'])
            # check to see if userID matches the playlist ownerID
            # if they match, then the user can edit or delete the playlist
            if userID == mPlaylist.ownerID:
                pass
            else:
                authStuatus = False
    except ValueError:
        authStatus = False
    return authStatus
        

# [START main_page]
class MainPage(webapp2.RequestHandler):
    def get(self):
        self.response.write("Welcome to the Podcast Lists API!")

# I followed the guidance in the Week 4 videos and implemented the patch
# capability for webapp2 by using the monkey patch provided in the answer at 
# https://stackoverflow.com/questions/16280496.
allowed_methods = webapp2.WSGIApplication.allowed_methods
new_allowed_methods = allowed_methods.union(('PATCH',))
webapp2.WSGIApplication.allowed_methods = new_allowed_methods

# [START app]
app = webapp2.WSGIApplication([
    ('/', MainPage),
    ('/playlist', PlaylistHandler),
    ('/playlists/([^\/]*)', PlaylistHandler),
    ('/playlists', MultPlaylistsHandler),
    ('/playlists/(.*)/podcasts', PodcastsInPlaylistHandler),
    ('/podcast', PodcastHandler),
    ('/podcasts/([^\/]*)', PodcastHandler),
    ('/podcasts', MultPodcastsHandler),
    ('/podcasts/(.*)/playlists', PlaylistsContainingPodcastHandler),
], debug=True)
# [END app]
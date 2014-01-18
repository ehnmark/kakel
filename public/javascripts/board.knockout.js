function PieceViewModel(piece, idx) {
	var pieceMap = { 0 : "piece", 1 : "piece-mine", 2 : "piece-theirs"}
	var self = this;
	self.piece = piece;
	self.idx = idx;
	self.state = piece.state;
	self.display = piece.display;
	self.pieceStyle = ko.computed(function() { return pieceMap[piece.state] });
}

function MainViewModel(gameId, playerId) {
	var self = this;

	self.gameId = gameId;
	self.playerId = playerId;
	self.pieces = ko.observableArray([]);
	self.selected = ko.observableArray([]);

	self.isMyTurn = ko.observable(false);
	self.canSubmitMove = ko.observable(false);

	self.refreshCanSubmitMove = function(p) {
		self.canSubmitMove(self.selected().length > 0);		
	}

	self.toggleSelected = function(p) {
		if(!self.isMyTurn()) {
			console.log("Not my turn; cannot toggle");
		} else if(p.state == 2) {
			console.log("Can't use pieces taken by opponent");
		} else {
			if(self.selected.indexOf(p) < 0)
				self.selected.push(p);
			else				
				self.selected.remove(p);

			self.refreshCanSubmitMove();
		}
	}

	self.updateStatus = function(message, isError) {
		$("#status").text(message);
		if(isError) {
			$("#status").color_fade({from:'red',to:'white'});
		} else {
			$("#status").color_fade();			
		}
	}

	self.submitMove = function() {
		var data = JSON.stringify(
			ko.utils.arrayMap(
				self.selected(), function(vm) { return vm.idx }));
		console.log("Submitting: " + data);

		$.ajax({
			url: "/move/" + gameId + "/" + playerId,
			type: "POST",
			data: data,
			contentType: "application/json; charset=utf-8",
			success: function(data, textStatus, xhr) {
				self.updateStatus(xhr.responseText, false);
			},
			error: function(xhr, textStatus, errorThrown) {
				self.updateStatus(xhr.responseText, true);
			}
		});
	}

	self.resizeBoard = function(pieces) {
		var count = pieces.length;
		var columns = Math.sqrt(count);
		var pieceWidth = $("li").first().outerWidth();
		var width = columns * pieceWidth;
		$("#board").width(width);
	}

	self.addPieces = function(pieces) {
		var i = 0;
		pieces.forEach(function(p) {				
			self.pieces.push(new PieceViewModel(p, i++));
		});
	}

	self.addLastMove = function(last) {
		if(last.length > 0) self.updateStatus("Last: " + last, false);
	}

	self.updateScore = function(me, opponent, standings) {
		var link = window.location.pathname.replace(me, opponent);
		$("#oplink").attr("href", link);
		$("#my-score").text(standings.me);
		$("#op-score").text(standings.opponent);
		if(standings.isOver) {
			if(standings.me > standings.opponent) {
				self.updateStatus("You won!", false);
			} else if(standings.me < standings.opponent) {
				self.updateStatus("You lost! Boo", true);
			} else {
				self.updateStatus("It's a tie", false);
			}
		}
	}

	self.reload = function() {
		self.pieces.removeAll();
		self.selected.removeAll();

		$.ajax({
			url: "/game/" + gameId + "/" + playerId,
			type: "GET",
			contentType: "application/json; charset=utf-8",
			success: function(data, textStatus, xhr) {
				self.isMyTurn(data.isMyTurn);
				self.addPieces(data.pieces);
				self.resizeBoard(data.pieces);
				self.addLastMove(data.last);
				self.refreshCanSubmitMove();
				self.updateScore(data.me, data.opponent, data.standings);
			},
			error: function(xhr, textStatus, errorThrown) {
				var error = xhr.responseText == undefined ? errorThrown : JSON.parse(xhr.responseText);
				self.updateStatus(error, true);
			}
		});		
	}
}

function createViewModel(gameId, playerId) {
	viewModel = new MainViewModel(gameId, playerId);

	ko.applyBindings(viewModel);
	$(document).ready(function() {
		viewModel.reload();
	});
}

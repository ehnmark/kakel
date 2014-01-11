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

	self.isMyTurn = ko.observable(false);
	self.gameId = gameId;
	self.playerId = playerId;
	self.pieces = ko.observableArray([]);
	self.selected = ko.observableArray([]);

	self.canSubmitMove = ko.observable(false);

	self.refreshCanSubmitMove = function(p) {
		self.canSubmitMove(self.selected().length > 0);		
	}

	self.toggleSelected = function(p) {
		if(!self.isMyTurn()) {
			console.log("Not my turn; cannot toggle");
		} else {
			if(self.selected.indexOf(p) < 0)
				self.selected.push(p);
			else				
				self.selected.remove(p);

			self.refreshCanSubmitMove();
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
			success: function(reply) {
				console.log("Got " + reply);
			},
			error: function(xhr, textStatus, errorThrown) {
				alert("error: " + textStatus + ", e: " + errorThrown)
			}
		});
	}

	self.reload = function() {
		self.pieces.removeAll();
		self.selected.removeAll();

		$.getJSON("/game/" + gameId + "/" + playerId, function(data) {
			self.isMyTurn(data.isMyTurn);
			i = 0;
			data.pieces.forEach(function(p) {				
				self.pieces.push(new PieceViewModel(p, i++));
			});
			self.refreshCanSubmitMove();
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
